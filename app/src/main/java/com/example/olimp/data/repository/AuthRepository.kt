package com.example.olimp.data.repository

import android.util.Log
import com.example.olimp.MyApplication
import com.example.olimp.data.models.*
import com.example.olimp.network.LoginRequest
import com.example.olimp.network.LoginResponse
import com.example.olimp.network.PasswordResetConfirmRequest
import com.example.olimp.network.PasswordResetConfirmResponse
import com.example.olimp.network.PasswordResetRequest
import com.example.olimp.network.RegisterRequest
import com.example.olimp.network.RegisterResponse
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.network.VerifyEmailRequest
import com.example.olimp.network.VerifyEmailResponse
import com.example.olimp.utils.SessionManager
import retrofit2.Response

class AuthRepository {

    companion object {
        private const val TAG = "AuthRepository"
    }

    private val sessionManager = SessionManager(MyApplication.context)
    private val api = RetrofitInstance.getApi(MyApplication.context)

    // Регистрация (username + email + password)
    suspend fun registerUser(
        username: String,
        email: String,
        password: String
    ): Response<RegisterResponse> {
        val request = RegisterRequest(username, email, password)
        Log.d(TAG, "Calling registerUser() with request: $request")
        return api.registerUser(request).also { response ->
            Log.d(
                TAG,
                "registerUser response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}"
            )
        }
    }

    // Логин по email
    suspend fun loginUser(email: String, password: String): Response<LoginResponse> {
        val request = LoginRequest(email, password)
        Log.d(TAG, "Calling loginUser() with request: $request")
        return api.loginUser(request).also { response ->
            Log.d(
                TAG,
                "loginUser response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}"
            )
            if (response.isSuccessful) {
                response.body()?.let {
                    sessionManager.saveAuthToken(it.token, email, it.user_id)
                    Log.d(TAG, "🔒 Токен сохранён после входа: ${sessionManager.getAuthToken()}")
                }
            }
        }
    }

    // Подтверждение email
    suspend fun verifyEmail(userId: Int, code: String): Response<VerifyEmailResponse> {
        val request = VerifyEmailRequest(userId, code)
        Log.d(TAG, "verifyEmail() with request: $request")
        return api.verifyEmail(request).also { response ->
            Log.d(
                TAG,
                "verifyEmail response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}"
            )
        }
    }

    suspend fun requestPasswordReset(email: String): Boolean {
        return try {
            Log.d(TAG, "Requesting password reset for email: $email")
            // Используем PasswordResetRequest вместо mapOf
            val request = PasswordResetRequest(email)
            val response = api.requestPasswordReset(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Password reset email sent successfully")
                true
            } else {
                Log.e(TAG, "Error in password reset: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in password reset request: ${e.message}")
            false
        }
    }

    suspend fun confirmPasswordReset(
        email: String,
        resetCode: String,
        newPassword: String
    ): Response<PasswordResetConfirmResponse> {
        // Создаём PasswordResetConfirmRequest вместо mapOf
        val request = PasswordResetConfirmRequest(email, resetCode, newPassword)
        Log.d(TAG, "Confirming password reset with request: $request")
        return api.confirmPasswordReset(request).also { response ->
            Log.d(
                TAG,
                "confirmPasswordReset response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}"
            )
        }
    }

    // Проверка существования пользователя по email
    suspend fun isUserExists(email: String): Boolean {
        return try {
            Log.d(TAG, "Checking if user exists with email: $email")
            val response = api.checkUserExists(email)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.exists == true) {
                    Log.d(TAG, "User exists: ${body.user_id}")
                    return true
                }
            }
            Log.d(TAG, "User does not exist")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user existence: ${e.message}")
            false
        }
    }

    // Получение данных пользователя по ID
    suspend fun getUserById(userId: Int): Response<UserResponse> {
        val token = sessionManager.getAuthToken()
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "❌ Нет токена для getUserById, userId: $userId")
            throw Exception("User is not authenticated")
        }

        Log.d(TAG, "Requesting user with ID: $userId, token: Token $token")
        Log.d(TAG, "Calling getUserById API for userId: $userId")

        return try {
            val response = api.getUserById(userId)
            Log.d(TAG, "Response received: ${response.code()} - ${response.message()}")
            if (response.isSuccessful) {
                val user = response.body()
                Log.d(TAG, "✅ Данные о пользователе получены: $user, avatar: ${user?.avatar}")
                user?.let { sessionManager.saveUserProfile(it) }
                response
            } else {
                Log.e(TAG, "❌ Ошибка getUserById: ${response.code()} - ${response.errorBody()?.string()}")
                response
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Сетевая ошибка вក: Сетевая ошибка в getUserById: ${e.message}")
            throw Exception("Network error: ${e.message}")
        }
    }
}