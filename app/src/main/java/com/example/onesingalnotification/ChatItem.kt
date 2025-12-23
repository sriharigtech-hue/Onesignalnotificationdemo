package com.example.onesingalnotification

sealed class ChatItem {

    // Message item holds a ChatModel
    data class MessageItem(val chat: ChatModel) : ChatItem()

    // Date item holds a string like "Today", "Yesterday" or "23 Dec 2025"
    data class DateItem(val dateText: String) : ChatItem()
}
