package com.example.onesingalnotification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class ChatActivity : AppCompatActivity() {
    private lateinit var etMsg: EditText
    private lateinit var btnSend: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val messageList = ArrayList<ChatModel>()
    private lateinit var receiverId: String
    // 🔹 SECOND USER ID (Use real UID from Firestore)
//    private val receiverId = "kcMpNTl4gDVNZfSdomI7qZe5ZmC2"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        receiverId = intent.getStringExtra("receiverId")!!

        etMsg = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        recyclerView = findViewById(R.id.recyclerChat)

        adapter = ChatAdapter(messageList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSend.setOnClickListener {
            if (etMsg.text.isNotEmpty()) {
                sendMessage(etMsg.text.toString())
                etMsg.setText("")
            }
        }

        loadMessages()

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
            "time" to System.currentTimeMillis()
        )

        // ✅ SAVE MESSAGE TO FIRESTORE
        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(map)
            .addOnSuccessListener {
                // ✅ AFTER SAVING MESSAGE → SEND PUSH
                sendOneSignalPush(receiverId, message)
            }
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
                        it.getString("text")!!
                    )
                    messageList.add(msg)
                }

                adapter.notifyDataSetChanged()
            }
    }
    private fun sendOneSignalPush(receiverId: String, message: String) {

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(receiverId)
            .get()
            .addOnSuccessListener { doc ->

                val oneSignalId = doc.getString("oneSignalId") ?: return@addOnSuccessListener
                println("OneSignal Targeting ID: $oneSignalId")
                if (oneSignalId.isNullOrEmpty()) {
                    println("❌ OneSignal ID Not Found")
                    return@addOnSuccessListener
                }

                val jsonBody = """
            {
              "app_id": "c6826e50-d417-4207-a901-b92979d03b03",
              "include_player_ids": ["$oneSignalId"],
              "headings": {"en": "New Message"},
              "contents": {"en": "$message"}
            }
        """.trimIndent()

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://onesignal.com/api/v1/notifications")
                    .addHeader(
                        "Authorization",
                        "Basic hscbqnzmnubh4y4irv7zifgsg"
                    )
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val res = response.body?.string()
                        println("✅ OneSignal Code: ${response.code}")
                        println("✅ OneSignal Response: $res")
                        runOnUiThread {
                            android.widget.Toast.makeText(
                                this@ChatActivity,
                                "✅ Push request sent to OneSignal",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                })
            }
    }


}