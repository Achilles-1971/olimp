package com.example.olimp.network

data class ResetPasswordRequest(
    val email: String,
    val newPassword: String
)
