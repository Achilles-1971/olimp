package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName

data class Friendship(
    val id: Int,
    val user: UserResponse,
    val friend: UserResponse,
    val status: String,
    val created_at: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null
)


