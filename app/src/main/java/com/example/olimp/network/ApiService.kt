package com.example.olimp.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(val username: String, val email: String, val password: String)
data class RegisterResponse(val user_id: Int, val message: String)

data class VerifyEmailRequest(val user_id: Int, val code: String)
data class VerifyEmailResponse(val message: String)

// Логин по email
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val user_id: Int, val token: String, val message: String)

interface ApiService {

    @POST("api/register/")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/login/")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/verify_email/")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<VerifyEmailResponse>

    @POST("password-reset/")  // Путь для запроса сброса пароля
    suspend fun requestPasswordReset(@Body request: Map<String, String>): Response<Map<String, String>>

    @POST("password-reset/confirm/")  // Путь для подтверждения сброса пароля
    suspend fun confirmPasswordReset(@Body request: Map<String, String>): Response<Map<String, String>>

    // Удален повторяющийся путь для сброса пароля
    // @POST("password-reset/")  // Путь для сброса пароля
    // suspend fun resetPassword(@Body request: Map<String, String>): Response<Map<String, String>>

    // Путь для подтверждения сброса пароля
    @POST("password-reset/verify-reset-code/")  // Новый путь для подтверждения кода сброса пароля
    suspend fun verifyResetCode(@Body request: Map<String, String>): Response<Map<String, String>>
}
