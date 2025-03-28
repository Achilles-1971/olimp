package com.example.olimp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.olimp.data.models.UserResponse

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "user_prefs"

        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_IS_EMAIL_CONFIRMED = "is_email_confirmed"
        private const val KEY_ROLE = "role"

        // Если в UserResponse есть поля bio, created_at, updated_at — добавим ключи:
        private const val KEY_BIO = "bio"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"
    }

    fun saveAuthToken(token: String, email: String, userId: Int) {
        val cleanToken = token.trim().removePrefix("Token ")
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, cleanToken)
            .putString(KEY_USER_EMAIL, email)
            .putInt(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        Log.d("SessionManager", "🔒 Сохранён токен: '$cleanToken'")
    }

    /**
     * Сохраняет данные пользователя (UserResponse) в SharedPreferences.
     */
    fun saveUserProfile(user: UserResponse) {
        Log.d("SessionManager", "Saving user profile: id=${user.id}, avatar=${user.avatar}")

        prefs.edit().apply {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_AVATAR, user.avatar)
            putBoolean(KEY_IS_EMAIL_CONFIRMED, user.isEmailConfirmed ?: false)
            putString(KEY_ROLE, user.role)
            // Дополнительные поля, если есть:
            putString(KEY_BIO, user.bio)
            putString(KEY_CREATED_AT, user.created_at)
            putString(KEY_UPDATED_AT, user.updated_at)

            apply()
        }
    }

    /**
     * Читает токен авторизации из SharedPreferences.
     */
    fun getAuthToken(): String? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)?.trim()
        Log.d("SessionManager", "🟢 Полученный токен: '$token'")
        return token
    }

    /**
     * Обновляет токен (убирая при этом префикс "Token ").
     */
    fun updateAuthToken(newToken: String) {
        val cleanToken = newToken.removePrefix("Token ").trim()
        prefs.edit().putString(KEY_AUTH_TOKEN, cleanToken).apply()
        Log.d("SessionManager", "Updated token: '$cleanToken'")
    }

    /**
     * Возвращает email пользователя, сохранённый как user_email.
     */
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    /**
     * Возвращает userId, если установлен (или null, если -1).
     */
    fun getUserId(): Int? = prefs.getInt(KEY_USER_ID, -1).takeIf { it != -1 }

    /**
     * Проверяет, залогинен ли пользователь.
     */
    fun isUserLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearSession() = prefs.edit().clear().apply()

    fun clearAuthToken() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
        Log.d("SessionManager", "🚫 Токен удалён, статус входа сброшен")
    }

    fun logout() {
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    /**
     * Возвращает объект UserResponse, если он полностью сохранён (id, username),
     * иначе возвращает null.
     */
    fun getUserProfile(): UserResponse? {
        val userId = getUserId() ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null

        val email = prefs.getString(KEY_EMAIL, null)
        val avatar = prefs.getString(KEY_AVATAR, null)
        val isEmailConfirmed = if (prefs.contains(KEY_IS_EMAIL_CONFIRMED)) {
            prefs.getBoolean(KEY_IS_EMAIL_CONFIRMED, false)
        } else null
        val role = prefs.getString(KEY_ROLE, null)
        val bio = prefs.getString(KEY_BIO, null)
        val createdAt = prefs.getString(KEY_CREATED_AT, null)
        val updatedAt = prefs.getString(KEY_UPDATED_AT, null)

        return UserResponse(
            id = userId,
            username = username,
            email = email,
            avatar = avatar,
            isEmailConfirmed = isEmailConfirmed,
            role = role,
            bio = bio,
            created_at = createdAt,
            updated_at = updatedAt
        )
    }
}
