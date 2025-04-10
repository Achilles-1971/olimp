package com.example.olimp.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.olimp.R
import com.example.olimp.ui.MainActivity
import com.example.olimp.ui.events.EventDetailActivity
import com.example.olimp.ui.messages.MessageActivity

object NotificationHelper {

    private const val CHANNEL_ID = "default_channel"
    private const val CHANNEL_NAME = "Уведомления"
    private const val CHANNEL_DESC = "Основной канал уведомлений"

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        eventId: String? = null,
        notifType: String? = null,
        senderId: String? = null
    ) {
        // Проверка разрешения на уведомления (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            val granted = context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w("NotificationHelper", "Нет разрешения на отправку уведомлений")
                return
            }
        }

        createNotificationChannel(context)

        val intent = when (notifType) {
            "new_message" -> {
                val userId = senderId?.toIntOrNull()
                if (userId == null || userId == 0) {
                    Log.e("NotificationHelper", "❌ Некорректный senderId: $senderId. Открываем MainActivity")
                    Intent(context, MainActivity::class.java)
                } else {
                    Log.d("NotificationHelper", "📨 Открываем MessageActivity с USER_ID=$userId")
                    Intent(context, MessageActivity::class.java).apply {
                        putExtra("USER_ID", userId)
                    }
                }
            }

            "event_comment", "event_comment_reply", "event_joined", "event_left", "event_reminder" -> {
                val parsedEventId = eventId?.toIntOrNull()
                if (parsedEventId == null || parsedEventId == 0) {
                    Log.e("NotificationHelper", "❌ Некорректный eventId: $eventId. Открываем MainActivity")
                    Intent(context, MainActivity::class.java)
                } else {
                    Log.d("NotificationHelper", "📨 Открываем EventDetailActivity с EVENT_ID=$parsedEventId")
                    Intent(context, EventDetailActivity::class.java).apply {
                        putExtra("EVENT_ID", parsedEventId)
                        // 💡 Можно передать коммент, если есть
                        if (notifType == "event_comment_reply") {
                            putExtra("SCROLL_TO_COMMENTS", true)
                        }
                    }
                }
            }

            "comment_liked" -> {
                val parsedEventId = eventId?.toIntOrNull()
                if (parsedEventId == null) {
                    Log.e("NotificationHelper", "❌ comment_liked без eventId")
                    Intent(context, MainActivity::class.java)
                } else {
                    Intent(context, EventDetailActivity::class.java).apply {
                        putExtra("EVENT_ID", parsedEventId)
                        putExtra("HIGHLIGHT_COMMENT", true)
                    }
                }
            }

            "event_submitted", "event_approved", "event_rejected" -> {
                Log.d("NotificationHelper", "📨 Открываем MyEventsActivity")
                Intent(context, com.example.olimp.ui.events.MyEventsActivity::class.java)
            }

            else -> {
                Log.w("NotificationHelper", "⚠️ Неизвестный тип уведомления: $notifType. Открываем MainActivity")
                Intent(context, MainActivity::class.java)
            }
        }.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val requestCode = System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications2)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}