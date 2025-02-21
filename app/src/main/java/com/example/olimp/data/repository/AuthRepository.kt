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

    // Метод для запроса сброса пароля
    suspend fun requestPasswordReset(email: String): Response<Map<String, String>> {
        val request = mapOf("email" to email)  // Параметры для запроса сброса пароля
        return api.requestPasswordReset(request).also { response ->
            Log.d(TAG, "requestPasswordReset response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }

    // Метод для подтверждения сброса пароля
    suspend fun confirmPasswordReset(email: String, resetCode: String, newPassword: String): Response<Map<String, String>> {
        val request = mapOf("email" to email, "reset_code" to resetCode, "new_password" to newPassword)  // Параметры для подтверждения сброса пароля
        return api.confirmPasswordReset(request).also { response ->
            Log.d(TAG, "confirmPasswordReset response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }

    // Метод для подтверждения кода сброса пароля
    suspend fun verifyResetCode(email: String, resetCode: String): Response<Map<String, String>> {
        val request: Map<String, String> = mapOf("email" to email, "reset_code" to resetCode)  // Параметры для подтверждения кода сброса
        return api.verifyResetCode(request).also { response ->
            Log.d(TAG, "verifyResetCode response code: ${response.code()}, body: ${response.body()}, errorBody: ${response.errorBody()?.string()}")
        }
    }
}
