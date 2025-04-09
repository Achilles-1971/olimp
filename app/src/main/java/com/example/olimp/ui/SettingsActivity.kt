package com.example.olimp.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.olimp.R
import com.example.olimp.network.ApiService
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchNotifications: Switch
    private lateinit var switchDarkMode: Switch
    private lateinit var switchPushEvents: Switch
    private lateinit var switchPushModeration: Switch
    private lateinit var switchPushLikes: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Инициализируем вьюхи
        switchNotifications = findViewById(R.id.switchNotifications)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchPushEvents = findViewById(R.id.switchPushEvents)
        switchPushModeration = findViewById(R.id.switchPushModeration)
        switchPushLikes = findViewById(R.id.switchPushLikes)

        // Локальные настройки
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)

        // 1. Сначала загружаем настройки с сервера (если эндпоинт есть)
        loadPushSettingsFromServer(sharedPref)

        // 2. Считываем локальные настройки (заглушка, если сервер недоступен)
        val notificationsEnabled = sharedPref.getBoolean("notifications_enabled", true)
        val darkModeEnabled = sharedPref.getBoolean("dark_mode_enabled", false)
        val pushEventsEnabled = sharedPref.getBoolean("push_events_enabled", true)
        val pushModerationEnabled = sharedPref.getBoolean("push_moderation_enabled", true)
        val pushLikesEnabled = sharedPref.getBoolean("push_likes_enabled", true)

        // 3. Устанавливаем значения переключателей
        switchNotifications.isChecked = notificationsEnabled
        switchDarkMode.isChecked = darkModeEnabled
        switchPushEvents.isChecked = pushEventsEnabled
        switchPushModeration.isChecked = pushModerationEnabled
        switchPushLikes.isChecked = pushLikesEnabled

        // 4. Листенеры переключателей
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("notifications_enabled", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Уведомления в приложении включены" else "Уведомления в приложении отключены",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Тёмная тема включена" else "Тёмная тема отключена",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchPushEvents.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("push_events_enabled", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Push уведомления о мероприятиях включены" else "Push уведомления о мероприятиях отключены",
                Toast.LENGTH_SHORT
            ).show()
            updatePushSetting("event", isChecked)
        }

        switchPushModeration.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("push_moderation_enabled", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Push уведомления о модерации включены" else "Push уведомления о модерации отключены",
                Toast.LENGTH_SHORT
            ).show()
            updatePushSetting("moderation", isChecked)
        }

        switchPushLikes.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("push_likes_enabled", isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Push уведомления о лайках включены" else "Push уведомления о лайках отключены",
                Toast.LENGTH_SHORT
            ).show()
            updatePushSetting("likes", isChecked)
        }
    }

    /**
     *  Загрузка push-настроек с сервера (если эндпоинт /api/get_push_settings/ доступен).
     *  После получения ответа мы обновляем локальные настройки в SharedPreferences.
     */
    private fun loadPushSettingsFromServer(sharedPref: android.content.SharedPreferences) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = RetrofitInstance.getApi(this@SettingsActivity)
                val response = apiService.getPushSettings() // <-- см. ApiService
                if (response.isSuccessful) {
                    val remoteSettings = response.body()
                    remoteSettings?.let {
                        withContext(Dispatchers.Main) {
                            // Обновляем переключатели
                            switchPushEvents.isChecked = it.events
                            switchPushModeration.isChecked = it.moderation
                            switchPushLikes.isChecked = it.likes_comments

                            // Сохраняем
                            sharedPref.edit()
                                .putBoolean("push_events_enabled", it.events)
                                .putBoolean("push_moderation_enabled", it.moderation)
                                .putBoolean("push_likes_enabled", it.likes_comments)
                                .apply()

                            Log.d("Settings", "✅ Push-настройки загружены с сервера")
                        }
                    }
                } else {
                    Log.e("Settings", "❌ Ошибка при getPushSettings: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Settings", "⚠️ Ошибка при загрузке push-настроек: ${e.message}")
            }
        }
    }

    /**
     *  Обновление push-настроек на сервере при переключении конкретного переключателя.
     */
    private fun updatePushSetting(category: String, enabled: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiService = RetrofitInstance.getApi(this@SettingsActivity)
                val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)

                // Считываем актуальные локальные значения
                val eventsEnabled =
                    if (category == "event") enabled
                    else sharedPref.getBoolean("push_events_enabled", true)

                val moderationEnabled =
                    if (category == "moderation") enabled
                    else sharedPref.getBoolean("push_moderation_enabled", true)

                val likesCommentsEnabled =
                    if (category == "likes") enabled
                    else sharedPref.getBoolean("push_likes_enabled", true)

                // Формируем объект для запроса
                val request = ApiService.PushSettingsRequest(
                    events = eventsEnabled,
                    moderation = moderationEnabled,
                    likesComments = likesCommentsEnabled
                )

                // Вызываем эндпоинт /api/update_push_settings/
                val response = apiService.updatePushSettings(request)
                if (response.isSuccessful) {
                    Log.d("Settings", "✅ Настройка '$category' обновлена на сервере: $enabled")
                } else {
                    Log.e("Settings", "❌ Ошибка обновления настройки '$category': ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Settings", "⚠️ Ошибка обновления настройки '$category': ${e.message}")
            }
        }
    }
}
