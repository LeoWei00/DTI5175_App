package com.example.shareat

data class Conversation(
    val chat_id: String = "",
    val participants: List<String> = emptyList(),
    val participant_names: List<String> = emptyList(),
    val post_id: String = "",
    val post_title: String = "",
    val last_message: String = "",
    val last_message_at: Long = 0L,
    val created_at: Long = 0L
)