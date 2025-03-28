package com.example.olimp.data.repository

import android.util.Log
import com.example.olimp.MyApplication
import com.example.olimp.data.models.UserResponse
import com.example.olimp.network.ApiService
import com.example.olimp.utils.SessionManager
import retrofit2.Response

class UserRepository(private val api: ApiService) {

    private val sessionManager = SessionManager(MyApplication.context)

    /**
     * Получает текущего пользователя, используя сохранённые токен и userId.
     * Если токен отсутствует или userId не установлен, выбрасывается исключение.
     * Теперь заголовок авторизации добавляется автоматически интерсептором, поэтому не передаём его вручную.
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

}
