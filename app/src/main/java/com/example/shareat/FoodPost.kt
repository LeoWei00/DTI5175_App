package com.example.shareat

data class FoodPost(
    var id: String = "",
    val post_id: String = "",
    val owner_id: String = "",
    val owner_name: String = "",
    val phone: String = "",
    val image_url: String = "",
    val title: String = "",
    val description: String = "",
    val ingredients: String = "",
    val category: String = "",
    val quantity: Int = 0,
    val unit: String = "",
    val dietary_labels: List<String> = emptyList(),
    val allergen_information: List<String> = emptyList(),
    val pickup_location: String = "",
    val pickup_date: String = "",
    val start_time: String = "",
    val end_time: String = "",
    val post_type: String = "",
    val status: String = "",
    val created_at: Long = 0L,
    val updated_at: Long = 0L
)