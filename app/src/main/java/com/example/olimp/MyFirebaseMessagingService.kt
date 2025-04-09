package com.example.olimp

import android.util.Log
import com.example.olimp.ui.notifications.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Новый токен: $token")
        // 🔄 Отправка токена на сервер, если не реализовано
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Достаём title и body из notification
        val title = remoteMessage.notification?.title ?: "Новое уведомление"
        val body = remoteMessage.notification?.body ?: "Пустое сообщение"

        // Достаём данные из data payload
        val dataMap = remoteMessage.data
        val notifType = dataMap["type"]
        val eventId = dataMap["event_id"] // Для событий
        val senderId = dataMap["sender_id"] // Для сообщений

        Log.d("FCM_MESSAGE", "Сообщение получено: $title - $body, data=$dataMap")

        // Показываем уведомление
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            eventId = eventId,
            notifType = notifType,
            senderId = senderId
        )
    }
}