package com.example.onesingalnotification

import android.content.Context
import android.content.Intent
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
import com.example.onesingalnotification.call.CallActivity
import com.google.firebase.firestore.SetOptions


import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChatActivity : AppCompatActivity() {
    private lateinit var etMsg: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatAdapter
    private val chatItemList = ArrayList<ChatItem>()

    private lateinit var receiverId: String
    private lateinit var tvTyping: TextView
    private lateinit var tvStickyDate: TextView
    private var lastStickyDate: String? = null
    private lateinit var tvUserName: TextView
    private lateinit var btnBack: ImageView
    private lateinit var tvLastSeen: TextView



    // 🔹 SECOND USER ID (Use real UID from Firestore)
//    private val receiverId = "kcMpNTl4gDVNZfSdomI7qZe5ZmC2"
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        receiverId = intent.getStringExtra("receiverId") ?: run {
            finish()
            return
        }

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
        tvUserName = findViewById(R.id.tvUserName)
        btnBack = findViewById(R.id.btnBack)
        tvLastSeen = findViewById(R.id.tvLastSeen)


        btnBack.setOnClickListener {
            finish()
        }


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

        loadReceiverName()
        loadMessages()
        listenTypingStatus()
        listenUserOnlineStatus()
        listenIncomingCalls()

        setupBottomTabs()
    }

    private fun setupBottomTabs() {
        val tabChat = findViewById<android.widget.LinearLayout>(R.id.tabChat)
        val tabCall = findViewById<android.widget.LinearLayout>(R.id.tabCall)
        val containerChat = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.containerChat)
        val containerCall = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.containerCall)
        val btnCallAction = findViewById<FloatingActionButton>(R.id.btnCallAction)
        val tvNameCall = findViewById<TextView>(R.id.tvNameCall)
        val imgProfileCall = findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imgProfileCall)

        // CHAT TAB CLICK
        tabChat.setOnClickListener {
            containerChat.visibility = View.VISIBLE
            containerCall.visibility = View.GONE
            updateTabStyles(isChatSelected = true)
        }

        // CALL TAB CLICK
        tabCall.setOnClickListener {
            containerChat.visibility = View.GONE
            containerCall.visibility = View.VISIBLE
            updateTabStyles(isChatSelected = false)

            // Bind Data to Call UI
            tvNameCall.text = tvUserName.text
            // Placeholder image for now as we don't have profile image URL in Firestore yet
             imgProfileCall.setImageResource(R.mipmap.ic_launcher) 
        }

        // CALL BUTTON ACTION
        btnCallAction.setOnClickListener {
            startVoiceCall(
                context = this,
                callerId = FirebaseAuth.getInstance().uid!!,
                callerName = tvUserName.text.toString(), // Using username from toolbar logic
                receiverId = receiverId,
                receiverOneSignalId = "" // Handled in startVoiceCall locally usually or modify to fetch
            )
        }
    }

    private fun updateTabStyles(isChatSelected: Boolean) {
        val tabChat = findViewById<android.widget.LinearLayout>(R.id.tabChat)
        val tabCall = findViewById<android.widget.LinearLayout>(R.id.tabCall)
        
        val chatIcon = tabChat.getChildAt(0) as ImageView
        val chatText = tabChat.getChildAt(1) as TextView
        val callIcon = tabCall.getChildAt(0) as ImageView
        val callText = tabCall.getChildAt(1) as TextView
        
        val colorActive = resources.getColor(R.color.primary_color)
        val colorInactive = 0xFF9E9E9E.toInt()

        if (isChatSelected) {
            chatIcon.setColorFilter(colorActive)
            chatText.setTextColor(colorActive)
            
            callIcon.setColorFilter(colorInactive)
            callText.setTextColor(colorInactive)
        } else {
            chatIcon.setColorFilter(colorInactive)
            chatText.setTextColor(colorInactive)
            
            callIcon.setColorFilter(colorActive)
            callText.setTextColor(colorActive)
        }
    }
    private fun listenIncomingCalls() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("calls")
            .whereEqualTo("receiverId", FirebaseAuth.getInstance().uid)
            .whereEqualTo("status", "calling")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val callId = doc.id
                        val callerName = doc.getString("callerName") ?: "Caller"
                        val channel = doc.getString("channel") ?: "default_channel"

                        val intent = Intent(this, CallActivity::class.java)
                        intent.putExtra("callId", callId)
                        intent.putExtra("channel", channel)
                        intent.putExtra("callerName", callerName)
                        startActivity(intent)
                    }
                }
            }
    }

    private fun listenUserOnlineStatus() {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(receiverId)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {

                    val isOnline = doc.getBoolean("online") ?: false

                    if (isOnline) {
                        tvLastSeen.text = "Online"
                        tvLastSeen.setTextColor(
                            resources.getColor(android.R.color.holo_green_dark)
                        )
                    } else {
                        val lastSeen = doc.getLong("lastSeen") ?: 0L
                        tvLastSeen.text = "Last seen ${formatLastSeen(lastSeen)}"
                        tvLastSeen.setTextColor(
                            resources.getColor(android.R.color.darker_gray)
                        )
                    }
                }
            }
    }
    private fun formatLastSeen(time: Long): String {
        if (time == 0L) return "recently"

        val now = System.currentTimeMillis()
        val diff = now - time

        val oneDay = 24 * 60 * 60 * 1000

        return when {
            diff < oneDay -> {
                val t = android.text.format.DateFormat.format("hh:mm a", time)
                "today at $t"
            }
            diff < 2 * oneDay -> {
                val t = android.text.format.DateFormat.format("hh:mm a", time)
                "yesterday at $t"
            }
            else -> {
                android.text.format.DateFormat.format("dd MMM, hh:mm a", time).toString()
            }
        }
    }

    private fun loadReceiverName() {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(receiverId)   // 👈 opposite user
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "Chat"
                    tvUserName.text = name
                } else {
                    tvUserName.text = "Chat"
                }
            }
            .addOnFailureListener {
                tvUserName.text = "Chat"
            }
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
                .addHeader("Authorization", "Basic os_v2_app_y2bg4uguc5bapkibxeuxtub3anwsc7lsdmoe6imnjdcbtaltdhz73oamlkxfzxyjgdcq2xh3m26hsx4hjg4aqkwjvc7debuoves7sfi")
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
    fun startVoiceCall(
        context: Context,
        callerId: String,
        callerName: String,
        receiverId: String,
        receiverOneSignalId: String
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val callDoc = firestore.collection("calls").document()
        val channelName = "channel_${System.currentTimeMillis()}" // unique channel

        val callData = hashMapOf(
            "callerId" to callerId,
            "callerName" to callerName,
            "receiverId" to receiverId,
            "channel" to channelName,
            "status" to "calling",
            "timestamp" to System.currentTimeMillis()
        )

        // Save call document
        callDoc.set(callData)

        // Send OneSignal push to receiver
        if (receiverOneSignalId.isNotEmpty()) {
            sendOneSignalCallPush(receiverOneSignalId, callerName)
        }

        // Open CallActivity
        val intent = Intent(context, CallActivity::class.java)
        intent.putExtra("callId", callDoc.id)
        intent.putExtra("channel", channelName)
        intent.putExtra("callerName", callerName)
        context.startActivity(intent)
    }

    fun sendOneSignalCallPush(oneSignalId: String, callerName: String) {
        val jsonBody = """
        {
          "app_id": "c6826e50-d417-4207-a901-b92979d03b03",
          "include_player_ids": ["$oneSignalId"],
          "headings": {"en": "Incoming Call"},
          "contents": {"en": "$callerName is calling you"}
        }
    """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://onesignal.com/api/v1/notifications")
            .addHeader("Authorization", "Basic os_v2_app_y2bg4uguc5bapkibxeuxtub3anwsc7lsdmoe6imnjdcbtaltdhz73oamlkxfzxyjgdcq2xh3m26hsx4hjg4aqkwjvc7debuoves7sfi")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
            override fun onResponse(call: Call, response: Response) { /* optional log */ }
        })
    }


}