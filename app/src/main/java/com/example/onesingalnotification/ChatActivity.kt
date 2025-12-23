package com.example.onesingalnotification

import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.text.TextWatcher
import android.view.View
import com.google.firebase.firestore.SetOptions


class ChatActivity : AppCompatActivity() {
    private lateinit var etMsg: EditText
    private lateinit var btnSend: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messageList = ArrayList<ChatModel>()
    private lateinit var receiverId: String
    private lateinit var tvTyping: TextView

    // 🔹 SECOND USER ID (Use real UID from Firestore)
//    private val receiverId = "kcMpNTl4gDVNZfSdomI7qZe5ZmC2"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        receiverId = intent.getStringExtra("receiverId") ?: return

        etMsg = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        recyclerView = findViewById(R.id.recyclerChat)

        adapter = ChatAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        tvTyping = findViewById(R.id.tvTyping)

        btnSend.setOnClickListener {
            if (etMsg.text.isNotEmpty()) {
                sendMessage(etMsg.text.toString())
                etMsg.setText("")
            }
        }
        etMsg.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTypingStatus(s?.isNotEmpty() == true)
            }
        })


        loadMessages()
        listenTypingStatus()

    }
    private fun sendMessage(message: String) {

        val senderId = FirebaseAuth.getInstance().uid!!

        val chatId =
            if (senderId < receiverId) "${senderId}_${receiverId}"
            else "${receiverId}_${senderId}"


        val map = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "text" to message,
            "time" to System.currentTimeMillis(),
            "status" to "sent"   // NEW
        )


        //  SAVE MESSAGE TO FIRESTORE
        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(map)
            .addOnSuccessListener {
                //  AFTER SAVING MESSAGE → SEND PUSH
                sendOneSignalPush(receiverId, message)
            }
        updateTypingStatus(false)

    }

    private fun loadMessages() {

        val senderId = FirebaseAuth.getInstance().uid!!

        val chatId =
            if (senderId < receiverId) "${senderId}_${receiverId}"
            else "${receiverId}_${senderId}"


        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("time")
            .addSnapshotListener { value, _ ->

                messageList.clear()

                value?.forEach {
                    val msg = ChatModel(
                        it.getString("senderId")!!,
                        it.getString("text")!!,
                        it.getLong("time") ?: 0L,
                        it.getString("status") ?: "sent"
                    )

                    messageList.add(msg)
                }

                adapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messageList.size - 1)

            }
    }
    private fun listenTypingStatus() {
        val myUid = FirebaseAuth.getInstance().uid!!
        val chatId =
            if (myUid < receiverId) "${myUid}_${receiverId}"
            else "${receiverId}_${myUid}"

        FirebaseFirestore.getInstance()
            .collection("typing")
            .document(chatId)
            .addSnapshotListener { value, _ ->
                if (value != null && value.exists()) {
                    val isTyping = value.getBoolean(receiverId) ?: false
                    tvTyping.visibility = if (isTyping) View.VISIBLE else View.GONE
                }
            }
    }

    private fun sendOneSignalPush(receiverId: String, message: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(receiverId).get().addOnSuccessListener { doc ->
            val oneSignalId = doc.getString("oneSignalId") ?: return@addOnSuccessListener
            if (oneSignalId.isEmpty()) return@addOnSuccessListener
            Log.d("OneSignalPush", "Receiver: $receiverId, OneSignalID: $oneSignalId")

            val jsonBody = """
                        {
                          "app_id": "c6826e50-d417-4207-a901-b92979d03b03",
                          "include_player_ids": ["$oneSignalId"],
                                                    
                              "android_group": "CHAT_$receiverId",
                              "android_group_message": {
                                "en": "New messages from chat"
                              },
                        
                          "headings": {"en": "New Message"},
                          "contents": {"en": "$message"}
                        }
                        """.trimIndent()


            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://onesignal.com/api/v1/notifications")
                .addHeader("Authorization", "Basic os_v2_app_y2bg4uguc5bapkibxeuxtub3ap47mc4j6e3ue5vwrulerlradw2dlh7vasrx5quf6httwcd3ngxc42sfag55a5yrwfxv22da6iet4uy")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
                override fun onResponse(call: Call, response: Response) {
                    val res = response.body?.string()
                    println("OneSignal Response Code: ${response.code}")
                    println("OneSignal Response Body: $res")
                }
            })
        }
    }
    override fun onStop() {
        super.onStop()
        updateTypingStatus(false)
    }


    private fun updateTypingStatus(isTyping: Boolean) {
        val myUid = FirebaseAuth.getInstance().uid!!
        val chatId =
            if (myUid < receiverId) "${myUid}_${receiverId}"
            else "${receiverId}_${myUid}"

        FirebaseFirestore.getInstance()
            .collection("typing")
            .document(chatId)
            .set(mapOf(myUid to isTyping), SetOptions.merge())

    }

}