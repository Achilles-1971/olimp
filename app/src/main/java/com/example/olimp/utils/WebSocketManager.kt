package com.example.olimp.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebSocketManager {
    private const val TAG = "WebSocketManager"
    private const val WS_URL = "ws://10.0.2.2:8000/ws/chat/" // Универсальный маршрут без userId
    private lateinit var client: OkHttpClient
    private var webSocket: WebSocket? = null
    private var isConnecting = false
    private lateinit var sessionManager: SessionManager

    // Слушатели для сообщений
    private val messageListeners = mutableListOf<(JSONObject) -> Unit>()

    // Инициализация менеджера
    fun init(context: Context) {
        sessionManager = SessionManager(context)
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    // Подключение к WebSocket
    fun connect() {
        if (isConnecting || webSocket != null) {
            Log.d(TAG, "Already connected or connecting")
            return
        }

        val token = sessionManager.getAuthToken() ?: run {
            Log.e(TAG, "No auth token available")
            return
        }

        val request = Request.Builder()
            .url(WS_URL) // Фиксированный URL: ws://10.0.2.2:8000/ws/chat/
            .addHeader("Authorization", "Token $token")
            .build()

        isConnecting = true
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnecting = false
                Log.i(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i(TAG, "Received: $text")
                val messageJson = JSONObject(text)
                notifyListeners(messageJson)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnecting = false
                Log.e(TAG, "WebSocket error: ${t.message}")
                this@WebSocketManager.webSocket = null
                reconnectWithDelay()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closing: $code / $reason")
                this@WebSocketManager.webSocket = null
            }
        })
    }

    // Отправка сообщения с указанием получателя
    fun sendMessage(content: String, toUserId: Int): Boolean {
        val json = JSONObject().apply {
            put("content", content)
            put("to_user_id", toUserId)
        }
        return webSocket?.send(json.toString())?.also {
            Log.i(TAG, "Sent: $json")
        } ?: run {
            Log.e(TAG, "WebSocket not connected")
            false
        }
    }

    // Добавление слушателя
    fun addMessageListener(listener: (JSONObject) -> Unit) {
        messageListeners.add(listener)
    }

    // Удаление слушателя
    fun removeMessageListener(listener: (JSONObject) -> Unit) {
        messageListeners.remove(listener)
    }

    // Уведомление всех слушателей
    private fun notifyListeners(message: JSONObject) {
        messageListeners.forEach { it.invoke(message) }
    }

    // Закрытие соединения
    fun disconnect() {
        webSocket?.close(1000, "App closed")
        webSocket = null
        messageListeners.clear()
        Log.i(TAG, "WebSocket disconnected")
    }

    // Проверка состояния
    fun isConnected(): Boolean = webSocket != null

    // Переподключение с задержкой
    private fun reconnectWithDelay(delayMillis: Long = 5000) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(delayMillis)
            Log.i(TAG, "🔁 Reconnecting WebSocket...")
            connect()
        }
    }
}