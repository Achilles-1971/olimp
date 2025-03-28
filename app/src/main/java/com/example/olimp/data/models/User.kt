package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName


data class UserResponse(
    val id: Int,
    val username: String,
    val email: String?,
    val avatar: String?,
    @SerializedName("is_email_confirmed")
    val isEmailConfirmed: Boolean?,
    val role: String?,
    val bio: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)


