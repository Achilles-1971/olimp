package com.example.olimp.data.repository

import android.util.Log
import com.example.olimp.network.*
import retrofit2.Response

class AuthRepository(private val api: ApiService) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // Регистрация (username + email + password)
    suspend fun registerUser(username: String, email: String, password: String): Response<RegisterResponse> {
        val request = RegisterRequest(username, email, password)
        Log.d(TAG, "Calling registerUser() with request: $request")
        return api.registerUser(request).also { response ->
            Log.d(TAG, "registerUser response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }

    // Логин по email
    suspend fun loginUser(email: String, password: String): Response<LoginResponse> {
        val request = LoginRequest(email, password)
        Log.d(TAG, "Calling loginUser() with request: $request")
        return api.loginUser(request).also { response ->
            Log.d(TAG, "loginUser response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }

    // Подтверждение email
    suspend fun verifyEmail(userId: Int, code: String): Response<VerifyEmailResponse> {
        val request = VerifyEmailRequest(userId, code)
        Log.d(TAG, "verifyEmail() with request: $request")
        return api.verifyEmail(request).also { response ->
            Log.d(TAG, "verifyEmail response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }

    // Запрос на сброс пароля (отправка reset-кода)
    suspend fun requestPasswordReset(email: String): Boolean {
        return try {
            val response = api.requestPasswordReset(mapOf("email" to email))
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

    // Метод для подтверждения сброса пароля (проверка reset-кода и установка нового пароля)
    suspend fun confirmPasswordReset(email: String, resetCode: String, newPassword: String): Response<Map<String, String>> {
        val request = mapOf("email" to email, "reset_code" to resetCode, "new_password" to newPassword)
        return api.confirmPasswordReset(request).also { response ->
            Log.d(TAG, "confirmPasswordReset response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }

    suspend fun isUserExists(email: String): Boolean {
        return try {
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
}
