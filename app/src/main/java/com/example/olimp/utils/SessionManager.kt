package com.example.olimp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUserSession(userId: Int, username: String, email: String, avatarUrl: String?, bio: String?, token: String) {
        prefs.edit()
            .putInt("USER_ID", userId)
            .putString("USERNAME", username)
            .putString("EMAIL", email)
            .putString("AVATAR_URL", avatarUrl)
            .putString("BIO", bio)
            .putString("TOKEN", token)
            .apply()
    }

    fun getUserId(): Int {
        return prefs.getInt("USER_ID", 0)
    }

    fun getUsername(): String {
        return prefs.getString("USERNAME", "Пользователь") ?: "Пользователь"
    }

    fun getEmail(): String {
        return prefs.getString("EMAIL", "") ?: ""
    }

    fun getAvatarUrl(): String {
        return prefs.getString("AVATAR_URL", "") ?: ""
    }

    fun getBio(): String {
        return prefs.getString("BIO", "") ?: ""
    }

    fun getToken(): String {
        return prefs.getString("TOKEN", "") ?: ""
    }

    fun isUserLoggedIn(): Boolean {
        return getToken().isNotEmpty()
    }

    fun updateUsername(newUsername: String) {
        prefs.edit().putString("USERNAME", newUsername).apply()
    }

    fun updateEmail(newEmail: String) {
        prefs.edit().putString("EMAIL", newEmail).apply()
    }

    fun updateUserBio(newBio: String) {
        prefs.edit().putString("BIO", newBio).apply()
    }

    fun updateUserAvatar(newAvatarUrl: String) {
        prefs.edit().putString("AVATAR_URL", newAvatarUrl).apply()
    }

    fun updateToken(newToken: String) {
        prefs.edit().putString("TOKEN", newToken).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
