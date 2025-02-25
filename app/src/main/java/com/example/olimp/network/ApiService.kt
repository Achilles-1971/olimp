package com.example.olimp.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

// Регистрация
data class RegisterRequest(val username: String, val email: String, val password: String)
data class RegisterResponse(val user_id: Int, val message: String)

// Логин
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val user_id: Int, val token: String, val message: String)

// Подтверждение email
data class VerifyEmailRequest(val user_id: Int, val code: String)
data class VerifyEmailResponse(val message: String)

// Проверка существования пользователя
data class CheckUserResponse(val exists: Boolean, val user_id: Int?)

interface ApiService {
    @POST("api/register/")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/login/")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/verify_email/")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<VerifyEmailResponse>

    @POST("api/password-reset/")
    suspend fun requestPasswordReset(@Body request: Map<String, String>): Response<Map<String, String>>

    @POST("api/password-reset/confirm/")
    suspend fun confirmPasswordReset(@Body request: Map<String, String>): Response<Map<String, String>>

    @GET("api/check_user_exists/")
    suspend fun checkUserExists(@Query("email") email: String): Response<CheckUserResponse>
}
