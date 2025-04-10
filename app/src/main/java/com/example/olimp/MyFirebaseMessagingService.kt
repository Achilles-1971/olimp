package com.example.olimp

import android.util.Log
import com.example.olimp.ui.notifications.NotificationHelper
import com.example.olimp.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "Новый токен: $token")
        // 💾 Сохраняем токен в SessionManager
        SessionManager(this).saveFcmToken(token)
        // 🔄 Также желательно сразу отправить его на сервер — сделаем позже
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Извлекаем title и body из уведомления
        val title = remoteMessage.notification?.title ?: "Новое уведомление"
        val body = remoteMessage.notification?.body ?: "Пустое сообщение"

        // 2. Извлекаем data payload
        val dataMap = remoteMessage.data
        val notifType = dataMap["type"]
        val eventId = dataMap["event_id"]
        val senderId = dataMap["sender_id"]
        val receiverId = dataMap["receiver_id"]

        // 3. Логируем всё для отладки
        Log.d("FCM_MESSAGE", "Получено FCM: $title - $body")
        Log.d("FCM_MESSAGE", "Data payload: $dataMap")

        // 4. Получаем текущий userId из SessionManager
        val sessionManager = SessionManager(applicationContext)
        val currentUserId = sessionManager.getUserId()
        if (currentUserId == null) {
            Log.w("FCM_MESSAGE", "Не удалось получить currentUserId — уведомление не обработано")
            return
        }

        // 5. Список поддерживаемых типов уведомлений
        val allowedTypes = listOf(
            "new_message",
            "event_approved",
            "event_submitted",
            "event_rejected",
            "event_joined",
            "event_left",
            "event_comment",
            "event_comment_reply",
            "event_reminder",
            "comment_liked"
        )

        // 6. Проверяем, что тип уведомления поддерживается и адресовано текущему пользователю
        if (notifType in allowedTypes && receiverId != null && receiverId.toIntOrNull() == currentUserId) {
            NotificationHelper.showNotification(
                context = applicationContext,
                title = title,
                body = body,
                notifType = notifType,
                eventId = eventId,
                senderId = senderId
            )
            Log.d("FCM_MESSAGE", "Уведомление показано: type=$notifType, для userId=$currentUserId")
        } else {
            Log.d("FCM_MESSAGE", "Уведомление пропущено: type=$notifType, receiverId=$receiverId, currentUserId=$currentUserId")
        }
    }
}
