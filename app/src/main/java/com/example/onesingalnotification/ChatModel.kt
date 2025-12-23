package com.example.onesingalnotification

data class ChatModel(
    val senderId: String = "",
    val message: String = "",
    val time: Long = 0L,
    val status: String = "sent"
)

