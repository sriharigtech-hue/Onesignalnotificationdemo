package com.example.onesingalnotification

data class UserModel(
    val uid: String = "",
    val name: String = "",
    val oneSignalId: String = "",
    val online: Boolean = false,
    val lastSeen: Long = 0L
)