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
    private val chatItemList = ArrayList<ChatItem>()

    private lateinit var receiverId: String
    private lateinit var tvTyping: TextView
    private lateinit var tvStickyDate: TextView
    private var lastStickyDate: String? = null


    // 🔹 SECOND USER ID (Use real UID from Firestore)
//    private val receiverId = "kcMpNTl4gDVNZfSdomI7qZe5ZmC2"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        receiverId = intent.getStringExtra("receiverId") ?: return

        etMsg = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        recyclerView = findViewById(R.id.recyclerChat)

        adapter = ChatAdapter(chatItemList)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        tvTyping = findViewById(R.id.tvTyping)
        tvStickyDate = findViewById(R.id.tvStickyDate)
        tvStickyDate.alpha = 1f
        tvStickyDate.visibility = View.VISIBLE


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePos != RecyclerView.NO_POSITION) {
                    var dateToShow: String? = null

                    // Find the nearest previous date header
                    for (i in firstVisiblePos downTo 0) {
                        val prevItem = chatItemList[i]
                        if (prevItem is ChatItem.DateItem) {
                            dateToShow = prevItem.dateText
                            break
                        }
                    }

                    dateToShow?.let { date ->
                        if (date != lastStickyDate) {
                            tvStickyDate.animate().alpha(0f).setDuration(100).withEndAction {
                                tvStickyDate.text = date
                                tvStickyDate.animate().alpha(1f).setDuration(200).start()
                            }.start()
                            lastStickyDate = date
                            tvStickyDate.visibility = View.VISIBLE
                        }
                    }
                }
            }
        })




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

                chatItemList.clear()

                var lastDate = ""

                value?.forEach {
                    val msg = ChatModel(
                        it.getString("senderId")!!,
                        it.getString("text")!!,
                        it.getLong("time") ?: 0L,
                        it.getString("status") ?: "sent"
                    )

                    val dateLabel = getDateLabel(msg.time)

                    // ADD DATE HEADER ONLY ONCE
                    if (dateLabel != lastDate) {
                        chatItemList.add(ChatItem.DateItem(dateLabel))
                        lastDate = dateLabel
                    }

                    // ADD MESSAGE
                    chatItemList.add(ChatItem.MessageItem(msg))
                }
                adapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(chatItemList.size - 1)


            }
        markMessagesDelivered(chatId)

    }
    private fun markMessagesDelivered(chatId: String) {
        val myUid = FirebaseAuth.getInstance().uid!!

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .whereEqualTo("receiverId", myUid)
            .whereEqualTo("status", "sent")
            .get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    doc.reference.update("status", "delivered")
                }
            }
    }

    private fun getDateLabel(time: Long): String {
        val now = System.currentTimeMillis()

        val oneDay = 24 * 60 * 60 * 1000
        return when {
            android.text.format.DateFormat.format("yyyyMMdd", time) ==
                    android.text.format.DateFormat.format("yyyyMMdd", now) -> {
                "Today"
            }

            android.text.format.DateFormat.format("yyyyMMdd", time) ==
                    android.text.format.DateFormat.format("yyyyMMdd", now - oneDay) -> {
                "Yesterday"
            }

            else -> {
                android.text.format.DateFormat.format("dd MMM yyyy", time).toString()
            }
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
    override fun onResume() {
        super.onResume()
        markMessagesSeen()
    }


    private fun markMessagesSeen() {
        val myUid = FirebaseAuth.getInstance().uid!!
        val chatId =
            if (myUid < receiverId) "${myUid}_${receiverId}"
            else "${receiverId}_${myUid}"

        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .whereEqualTo("receiverId", myUid)
            .whereEqualTo("status", "delivered")
            .get()
            .addOnSuccessListener {
                for (doc in it.documents) {
                    doc.reference.update("status", "seen")
                }
            }
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