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
import okhttp3.ResponseBody.Companion.toResponseBody
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
        return api.cancelFriendRequest(request) // вызов метода API
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

    suspend fun acceptFriendRequest(friendshipId: Int): Response<Unit> {
        val body = FriendRequest(friendship_id = friendshipId)
        return api.acceptFriend(body)
    }

    suspend fun getUserById(id: Int): Response<UserResponse> {
        Log.d("UserRepository", "Получение пользователя с ID: $id")
        return api.getUserById(id)
    }

    suspend fun getFriendRequests(): Response<List<Friendship>> {
        Log.d("UserRepository", "📥 Получение входящих заявок в друзья")
        return api.getFriendships("pending") // Используем getFriendships с параметром "pending"
    }

    suspend fun getFriends(): Response<List<Friendship>> {
        Log.d("UserRepository", "🤝 Получение списка друзей")
        return api.getFriendships("accepted") // Используем getFriendships с параметром "accepted"
    }

    suspend fun getFriendList(): Response<List<Friendship>> {
        return api.getFriendships(filter = "accepted")
    }

}