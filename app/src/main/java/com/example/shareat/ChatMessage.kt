package com.example.shareat

data class ChatMessage(
    val message_id: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val text: String = "",
    val sent_at: Long = 0L
)