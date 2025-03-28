package com.example.olimp.network

import com.example.olimp.data.models.*
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// Модель для запроса на регистрацию
data class RegisterRequest(val username: String, val email: String, val password: String)
// Модель для ответа на регистрацию
data class RegisterResponse(val user_id: Int, val message: String)

// Модель для запроса на вход
data class LoginRequest(val email: String, val password: String)
// Модель для ответа на вход
data class LoginResponse(val user_id: Int, val token: String, val message: String)

// Модель для запроса на подтверждение email
data class VerifyEmailRequest(val user_id: Int, val code: String)
// Модель для ответа на подтверждение email
data class VerifyEmailResponse(val message: String)

// Модель для ответа на проверку существования пользователя
data class CheckUserResponse(val exists: Boolean, val user_id: Int?)

// Новая модель для ответа на запрос сброса пароля
data class PasswordResetRequest(val email: String)
data class PasswordResetResponse(val message: String)

// Новая модель для подтверждения сброса пароля
data class PasswordResetConfirmRequest(
    val email: String,
    @SerializedName("reset_code") val resetCode: String,
    @SerializedName("new_password") val newPassword: String
)
data class PasswordResetConfirmResponse(val message: String)

// Новая модель для ответа на добавление просмотра
data class ViewResponse(
    @SerializedName("views_count") val viewsCount: Int
)

// Существующий sealed class для обработки ответов
sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: Int, val message: String?) : ApiResponse<Nothing>()
    object NetworkError : ApiResponse<Nothing>()
}

interface ApiService {

    // Аутентификация и регистрация
    @POST("api/register/")
    suspend fun registerUser(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/login/")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/verify_email/")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<VerifyEmailResponse>

    @POST("api/password-reset/")
    suspend fun requestPasswordReset(@Body request: PasswordResetRequest): Response<PasswordResetResponse>

    @POST("api/password-reset/confirm/")
    suspend fun confirmPasswordReset(@Body request: PasswordResetConfirmRequest): Response<PasswordResetConfirmResponse>

    @GET("api/check_user_exists/")
    suspend fun checkUserExists(@Query("email") email: String): Response<CheckUserResponse>

    // Пользователи (авторизация теперь происходит через интерсептор)
    @GET("api/users/{id}/")
    suspend fun getUserById(
        @Path("id") id: Int
    ): Response<UserResponse>
    // Примечание: Логи для этого метода добавлены в AuthRepository.kt и RetrofitInstance.kt

    // Новости
    @GET("api/news/")
    suspend fun getNews(
        @Query("sort") sort: String? = null
    ): Response<List<News>>

    @GET("api/news/{id}/")
    suspend fun getNewsDetail(
        @Path("id") newsId: Int
    ): Response<News>

    @POST("api/news/{id}/add_view/")
    suspend fun addView(
        @Path("id") newsId: Int
    ): Response<ViewResponse>

    @POST("api/news/{id}/like/")
    suspend fun addLike(
        @Path("id") newsId: Int
    ): Response<LikeResponse> // Уже правильно настроен

    // Комментарии
    @GET("api/comments/")
    suspend fun getComments(
        @Query("entity_type") entityType: String = "news",
        @Query("entity_id") newsId: Int,
        @Query("page") page: Int? = null
    ): Response<PaginatedCommentsResponse>

    @GET("api/comments/latest/{news_id}/")
    suspend fun getLatestComment(
        @Path("news_id") newsId: Int
    ): Response<List<Comment>>

    @POST("api/comments/create/")
    suspend fun createComment(
        @Body comment: CommentRequest
    ): Response<Comment>

    @PUT("api/comments/{id}/update/")
    suspend fun updateComment(
        @Path("id") commentId: Int,
        @Body comment: CommentRequest
    ): Response<Comment>

    @DELETE("api/comments/{id}/delete/")
    suspend fun deleteComment(
        @Path("id") commentId: Int
    ): Response<Map<String, String>>

    @POST("api/comments/{id}/like_toggle/")
    suspend fun toggleCommentLike(
        @Path("id") commentId: Int
    ): Response<CommentLikeToggleResponse>

    @GET("api/events/")
    suspend fun getEvents(
        @Query("filter") filter: String? = null
    ): Response<List<Event>>

    @POST("api/events/create/")
    suspend fun createEvent(
        @Body request: CreateEventRequest
    ): Response<Event>

    // Если есть удаление:
    @DELETE("api/events/{id}/delete/")
    suspend fun deleteEvent(
        @Path("id") eventId: Int
    ): Response<Unit>

    @Multipart
    @POST("api/events/{id}/photos/add/")
    suspend fun uploadEventPhotos(
        @Path("id") eventId: Int,
        @Part photos: List<MultipartBody.Part>
    ): Response<List<EventPhotoResponse>>

    @GET("api/events/{id}/")
    suspend fun getEventDetail(
        @Path("id") eventId: Int
    ): Response<Event>

}