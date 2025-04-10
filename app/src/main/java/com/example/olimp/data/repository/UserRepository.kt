package com.example.olimp.data.repository

import android.util.Log
import com.example.olimp.MyApplication
import com.example.olimp.data.models.Friendship
import com.example.olimp.data.models.UserResponse
import com.example.olimp.network.ApiService
import com.example.olimp.network.FriendRequest
import com.example.olimp.network.FriendRequestParams
import com.example.olimp.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class UserRepository(private val api: ApiService) {

    private val sessionManager = SessionManager(MyApplication.context)

    /**
     * Получает текущего пользователя, используя сохранённые токен и userId.
     */
    suspend fun getCurrentUser(): Response<UserResponse> {
        val token = sessionManager.getAuthToken()
        val userId = sessionManager.getUserId()

        Log.d("UserRepository", "🔍 Токен: $token")
        Log.d("UserRepository", "🔍 userId: $userId")

        if (token.isNullOrEmpty() || userId == null) {
            Log.e("UserRepository", "❌ Пользователь не аутентифицирован")
            throw Exception("Пользователь не аутентифицирован")
        }

        Log.d("UserRepository", "✅ Получение пользователя с ID: $userId")
        return api.getUserById(userId)
    }

    /**
     * Получает список всех пользователей.
     * @return Response с списком пользователей или ошибкой.
     */
    suspend fun getAllUsers(): Response<List<UserResponse>> {
        Log.d("UserRepository", "🔍 Получение списка всех пользователей")
        return api.getAllUsers()
    }

    /**
     * Получает список дружеских связей текущего пользователя.
     * @return Response с списком дружеских связей или ошибкой.
     */
    suspend fun getFriendships(): Response<List<Friendship>> {
        Log.d("UserRepository", "🔍 Получение списка дружеских связей")
        return api.getFriendships()
    }

    /**
     * Отправляет заявку в друзья.
     * @param friendId ID пользователя, которому отправляем заявку.
     * @return Response с результатом запроса.
     */
    suspend fun sendFriendRequest(friendId: Int): Response<Unit> {
        val currentUserId = sessionManager.getUserId() ?: throw Exception("Пользователь не аутентифицирован")
        val request = FriendRequestParams(user_id = currentUserId, friend_id = friendId)
        Log.d("UserRepository", "Отправка заявки от $currentUserId к $friendId")
        return api.sendFriendRequest(request)
    }

    /**
     * Отменяет заявку в друзья или удаляет дружбу.
     * @param friendId ID пользователя, с которым нужно отменить связь.
     * @return Response с результатом запроса.
     */
    suspend fun cancelFriendRequest(friendId: Int): Response<Unit> {
        val currentUserId = sessionManager.getUserId() ?: throw Exception("Пользователь не аутентифицирован")
        val request = FriendRequestParams(user_id = currentUserId, friend_id = friendId)
        Log.d("UserRepository", "Отмена заявки между $currentUserId и $friendId")
        return api.cancelFriendRequest(request)
    }

    /**
     * Принимает заявку в друзья.
     * @param friendshipId ID дружеской связи.
     * @return Response с результатом запроса.
     */
    suspend fun acceptFriend(friendshipId: Int): Response<Unit> {
        val request = FriendRequest(friendship_id = friendshipId)
        Log.d("UserRepository", "Принятие заявки с ID: $friendshipId")
        return api.acceptFriend(request)
    }

    /**
     * Удаляет дружескую связь.
     * @param friendshipId ID дружеской связи.
     * @return Response с результатом запроса.
     */
    suspend fun removeFriend(friendshipId: Int): Response<Unit> {
        val request = FriendRequest(friendship_id = friendshipId)
        Log.d("UserRepository", "Удаление дружбы с ID: $friendshipId")
        return api.removeFriend(request)
    }

    /**
     * Принимает заявку в друзья (альтернативный метод).
     * @param friendshipId ID дружеской связи.
     * @return Response с результатом запроса.
     */
    suspend fun acceptFriendRequest(friendshipId: Int): Response<Unit> {
        val body = FriendRequest(friendship_id = friendshipId)
        return api.acceptFriend(body)
    }

    /**
     * Получает пользователя по ID.
     * @param id ID пользователя.
     * @return Response с данными пользователя или ошибкой.
     */
    suspend fun getUserById(id: Int): Response<UserResponse> {
        Log.d("UserRepository", "Получение пользователя с ID: $id")
        return api.getUserById(id)
    }

    /**
     * Получает список входящих заявок в друзья.
     * @return Response с списком заявок или ошибкой.
     */
    suspend fun getFriendRequests(): Response<List<Friendship>> {
        Log.d("UserRepository", "📥 Получение входящих заявок в друзья")
        return api.getFriendships("pending")
    }

    /**
     * Получает список друзей.
     * @return Response с списком друзей или ошибкой.
     */
    suspend fun getFriends(): Response<List<Friendship>> {
        Log.d("UserRepository", "🤝 Получение списка друзей")
        return api.getFriendships("accepted")
    }

    /**
     * Получает список друзей (альтернативный метод).
     * @return Response с списком друзей или ошибкой.
     */
    suspend fun getFriendList(): Response<List<Friendship>> {
        return api.getFriendships(filter = "accepted")
    }

    /**
     * Обновляет профиль пользователя (username, bio, avatar).
     * @param userId ID пользователя.
     * @param fields Карта текстовых полей (username, bio).
     * @param avatar Аватарка в формате MultipartBody.Part (опционально).
     * @return Response с обновленными данными пользователя или ошибкой.
     */
    suspend fun updateUserProfile(
        userId: Int,
        fields: Map<String, String>,
        avatar: MultipartBody.Part? = null
    ): Response<UserResponse> {
        val textFields = fields.mapValues {
            it.value.toRequestBody("text/plain".toMediaTypeOrNull())
        }
        Log.d("UserRepository", "Обновление профиля для userId: $userId, fields: $fields, avatar: ${avatar != null}")
        return api.updateUserProfile(userId, textFields, avatar)
    }
}