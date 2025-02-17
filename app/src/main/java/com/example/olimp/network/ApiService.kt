package com.example.olimp.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// Модель запроса на регистрацию
data class RegisterRequest(val username: String, val email: String, val password: String)
data class RegisterResponse(val user_id: Int, val message: String)

// Модель запроса на подтверждение e-mail
data class VerifyEmailRequest(val user_id: Int, val code: String)
data class VerifyEmailResponse(val message: String)

// Модель запроса на вход
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val message: String, val error: String?)

// Модель данных пользователя
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val avatar: String?,
    val bio: String?
)

// Интерфейс API
interface ApiService {
    @POST("api/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/verify_email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<VerifyEmailResponse>

    @POST("api/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/users")
    suspend fun getUsers(): Response<List<UserResponse>>

    @Multipart
    @PUT("api/users/{id}/update")
    suspend fun updateUserProfile(
        @Path("id") userId: Int,
        @Part("bio") bio: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Response<Unit>
}
