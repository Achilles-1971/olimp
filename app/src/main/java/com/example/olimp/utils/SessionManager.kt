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
        private const val KEY_BIO = "bio"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_LOCATION_PERMISSION_REQUESTED = "location_permission_requested"
        private const val KEY_FCM_TOKEN = "fcm_token" // Новый ключ для FCM-токена
    }

    fun saveAuthToken(token: String, email: String, userId: Int) {
        val cleanToken = token.trim().removePrefix("Token ")
        with(prefs.edit()) {
            putString(KEY_AUTH_TOKEN, cleanToken)
            putString(KEY_USER_EMAIL, email)
            putInt(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Log.d("SessionManager", "🔒 Сохранён токен: '$cleanToken', email: '$email', userId: $userId")
    }

    /**
     * Сохраняет данные пользователя (UserResponse) в SharedPreferences.
     */
    fun saveUserProfile(user: UserResponse) {
        Log.d("SessionManager", "Сохранение профиля: id=${user.id}, username=${user.username}, email=${user.email}, avatar=${user.avatar}")
        with(prefs.edit()) {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_AVATAR, user.avatar)
            user.isEmailConfirmed?.let { putBoolean(KEY_IS_EMAIL_CONFIRMED, it) }
            putString(KEY_ROLE, user.role)
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
        Log.d("SessionManager", "🔄 Обновлён токен: '$cleanToken'")
    }

    /**
     * Возвращает email пользователя, сохранённый как user_email.
     */
    fun getUserEmail(): String? {
        val email = prefs.getString(KEY_USER_EMAIL, null)
        Log.d("SessionManager", "📧 Получен email: '$email'")
        return email
    }

    /**
     * Возвращает userId, если установлен (или null, если -1).
     */
    fun getUserId(): Int? {
        val userId = prefs.getInt(KEY_USER_ID, -1).takeIf { it != -1 }
        Log.d("SessionManager", "🆔 Получен userId: $userId")
        return userId
    }

    /**
     * Проверяет, залогинен ли пользователь.
     */
    fun isUserLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d("SessionManager", "🔐 Статус входа: $isLoggedIn")
        return isLoggedIn
    }

    /**
     * Очищает всю сессию.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
        Log.d("SessionManager", "🧹 Вся сессия очищена")
    }

    /**
     * Удаляет только токен и статус входа.
     */
    fun clearAuthToken() {
        with(prefs.edit()) {
            remove(KEY_AUTH_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        Log.d("SessionManager", "🚫 Токен удалён, статус входа сброшен")
    }

    /**
     * Выход из системы (аналог clearAuthToken).
     */
    fun logout() {
        with(prefs.edit()) {
            remove(KEY_AUTH_TOKEN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        Log.d("SessionManager", "🏃 Выход из системы выполнен")
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

        val user = UserResponse(
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
        Log.d("SessionManager", "👤 Профиль пользователя восстановлен: id=$userId, username=$username")
        return user
    }

    /**
     * Проверяет, был ли уже выполнен запрос разрешения на местоположение.
     */
    fun hasRequestedLocationPermission(): Boolean {
        val requested = prefs.getBoolean(KEY_LOCATION_PERMISSION_REQUESTED, false)
        Log.d("SessionManager", "📍 Был ли запрос разрешения на местоположение: $requested")
        return requested
    }

    /**
     * Устанавливает флаг, что запрос разрешения на местоположение был выполнен.
     */
    fun setLocationPermissionRequested(requested: Boolean) {
        with(prefs.edit()) {
            putBoolean(KEY_LOCATION_PERMISSION_REQUESTED, requested)
            apply()
        }
        Log.d("SessionManager", "📍 Установлен флаг запроса разрешения на местоположение: $requested")
    }

    /**
     * Сохраняет FCM-токен в SharedPreferences.
     */
    fun saveFcmToken(token: String) {
        with(prefs.edit()) {
            putString(KEY_FCM_TOKEN, token)
            apply()
        }
        Log.d("SessionManager", "🔥 Сохранён FCM-токен: '$token'")
    }

    /**
     * Получает FCM-токен из SharedPreferences.
     */
    fun getFcmToken(): String? {
        val token = prefs.getString(KEY_FCM_TOKEN, null)
        Log.d("SessionManager", "🔥 Получен FCM-токен: '$token'")
        return token
    }


}