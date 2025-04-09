package com.example.olimp.network

import com.example.olimp.data.models.*  // Используем модели из data.models
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

// Модель для запроса сброса пароля
data class PasswordResetRequest(val email: String)
data class PasswordResetResponse(val message: String)

// Модель для подтверждения сброса пароля
data class PasswordResetConfirmRequest(
    val email: String,
    @SerializedName("reset_code") val resetCode: String,
    @SerializedName("new_password") val newPassword: String
)
data class PasswordResetConfirmResponse(val message: String)

// Модель для ответа на добавление просмотра
data class ViewResponse(
    @SerializedName("views_count") val viewsCount: Int
)

// Модель для ответа на регистрацию на событие
data class EventRegistrationResponse(
    val id: Int,
    @SerializedName("event") val eventId: Int,
    @SerializedName("user") val userId: Int,
    val status: String,
    @SerializedName("registered_at") val registeredAt: String
)

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    data class Error(val code: Int, val message: String?) : ApiResponse<Nothing>()
    object NetworkError : ApiResponse<Nothing>()
}

data class FriendRequest(
    val friendship_id: Int
)
data class FriendRequestParams(val user_id: Int, val friend_id: Int)

interface ApiService {

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

    // Пользователи
    @GET("api/users/{id}/")
    suspend fun getUserById(
        @Path("id") id: Int
    ): Response<UserResponse>

    @GET("api/users/")
    suspend fun getAllUsers(): Response<List<UserResponse>>

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
    ): Response<LikeResponse>

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
    ): Response<List<Comment>>

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

    // События
    @GET("api/events/")
    suspend fun getEvents(
        @Query("filter") filter: String? = null
    ): Response<List<Event>>

    @POST("api/events/create/")
    suspend fun createEvent(
        @Body request: CreateEventRequest
    ): Response<Event>

    @DELETE("api/events/{id}/delete/")
    suspend fun deleteEvent(@Path("id") eventId: Int): Response<Unit>

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

    @POST("api/events/{id}/register/")
    suspend fun registerForEvent(
        @Path("id") eventId: Int
    ): Response<EventRegistrationResponse>

    @DELETE("api/events/{id}/register/")
    suspend fun cancelParticipation(
        @Path("id") eventId: Int
    ): Response<Unit>

    @Multipart
    @PATCH("api/events/{id}/update_preview/")
    suspend fun updateEventPreview(
        @Path("id") eventId: Int,
        @Part image: MultipartBody.Part
    ): Response<Event>

    @GET("api/notifications/")
    suspend fun getNotifications(): Response<List<NotificationModel>>

    @DELETE("api/notifications/{id}/delete/")
    suspend fun deleteNotification(@Path("id") id: Int): Response<Unit>

    @GET("api/events/my_events/")
    suspend fun myEvents(): Response<List<Event>>

    @POST("api/register_fcm_token/")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<FcmTokenResponse>

    data class FcmTokenRequest(val token: String)
    data class FcmTokenResponse(
        val message: String,
        val id: Int,
        val user: Int,
        val token: String,
        val created_at: String?,
        val updated_at: String?
    )

    @POST("api/update_push_settings/")
    suspend fun updatePushSettings(@Body settings: PushSettingsRequest): Response<Unit>

    data class PushSettingsRequest(
        @SerializedName("events") val events: Boolean,
        @SerializedName("moderation") val moderation: Boolean,
        @SerializedName("likes_comments") val likesComments: Boolean
    )

    data class PushSettingsResponse(
        val events: Boolean,
        val moderation: Boolean,
        @SerializedName("likes_comments") val likes_comments: Boolean
    )

    @GET("api/get_push_settings/")
    suspend fun getPushSettings(): Response<PushSettingsResponse>

    // Дружба
    @GET("api/list_user_friendships/")
    suspend fun getFriendships(): Response<List<Friendship>>

    @POST("api/friendships/accept/")
    suspend fun acceptFriend(@Body request: FriendRequest): Response<Unit>

    @HTTP(method = "DELETE", path = "api/friendships/remove/", hasBody = true)
    suspend fun removeFriend(@Body request: FriendRequest): Response<Unit>

    @POST("api/friendships/add/")
    suspend fun sendFriendRequest(@Body request: FriendRequestParams): Response<Unit>

    @HTTP(method = "DELETE", path = "api/friendships/remove/", hasBody = true)
    suspend fun cancelFriendRequest(@Body request: FriendRequestParams): Response<Unit>

    @POST("api/friendships/accept/{user_id}/")
    suspend fun acceptFriendRequest(@Path("user_id") userId: Int): Response<Unit>

    @GET("api/list_user_friendships/")
    suspend fun getFriendships(
        @Query("filter") filter: String? = null
    ): Response<List<Friendship>>

    @POST("api/messages/send/")
    suspend fun sendMessage(@Body request: MessageRequest): Response<MessageResponse>

    @GET("api/messages/between/{user1}/{user2}/")
    suspend fun getMessagesBetween(
        @Path("user1") user1: Int,
        @Path("user2") user2: Int,
        @Query("page") page: Int? = null
    ): Response<PaginatedMessagesResponse>

    @POST("api/messages/{message_id}/read/")
    suspend fun markMessageRead(@Path("message_id") messageId: Int): Response<MessageResponse>

    @GET("api/messages/conversations/")
    suspend fun listConversations(): Response<List<ConversationResponse>>
}