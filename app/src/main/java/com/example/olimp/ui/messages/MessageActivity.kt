package com.example.olimp.ui.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.data.models.MessageResponse
import com.example.olimp.data.models.UserResponse
import com.example.olimp.data.repository.MessagesRepository
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.ActivityMessageBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.messages.adapter.MessageAdapter
import com.example.olimp.utils.ChatSession
import com.example.olimp.utils.SessionManager
import com.example.olimp.utils.WebSocketManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding
    private lateinit var messagesRepository: MessagesRepository
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MessageAdapter

    private var otherUserId: Int = 0
    private val messageIds = mutableSetOf<Int>()

    private val messageListener: (JSONObject) -> Unit = { messageJson ->
        val messageData = messageJson.getJSONObject("message_data")
        val message = parseMessageResponse(messageData)
        if (message.toUser == otherUserId || message.fromUser.id == otherUserId) {
            runOnUiThread {
                if (message.fromUser.id == sessionManager.getUserId()) {
                    adapter.replaceTemporaryMessage(message)
                } else {
                    if (messageIds.add(message.id)) {
                        adapter.addMessage(message) {
                            binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }
            }
            if (message.toUser == sessionManager.getUserId() && message.readAt == null) {
                markMessageAsRead(message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiService = RetrofitInstance.getApi(this)
        messagesRepository = MessagesRepository(apiService)
        userRepository = UserRepository(apiService)
        sessionManager = SessionManager(this)

        updateOtherUserId(intent)
        setupRecyclerView()
        setupInput()
        loadChatTitle()
        loadMessages()
        WebSocketManager.addMessageListener(messageListener)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val newUserId = intent?.getIntExtra("USER_ID", 0) ?: 0
        if (newUserId != otherUserId && newUserId != 0) {
            Log.d("MessageActivity", "onNewIntent: новый пользователь $newUserId")
            setIntent(intent)
            updateOtherUserId(intent)
            loadChatTitle()
            loadMessages()
            messageIds.clear()
            adapter.submitList(emptyList())
        }
    }

    private fun updateOtherUserId(intent: Intent?) {
        val newUserId = intent?.getIntExtra("USER_ID", 0) ?: 0
        if (newUserId == 0) {
            Log.e("MessageActivity", "otherUserId is 0, что-то не так с Intent")
        } else if (newUserId != otherUserId) {
            otherUserId = newUserId
            ChatSession.currentChatUserId = otherUserId
            Log.d("MessageActivity", "Updated otherUserId to: $otherUserId")
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(sessionManager.getUserId() ?: 0)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChat.adapter = adapter
    }

    private fun setupInput() {
        binding.btnBack.setOnClickListener { finishWithResult() }
        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendMessage(content)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun loadChatTitle() {
        if (otherUserId == 0) {
            binding.tvChatTitle.text = "Ошибка: пользователь не определён"
            return
        }
        lifecycleScope.launch {
            try {
                val response = userRepository.getUserById(otherUserId)
                if (response.isSuccessful) {
                    binding.tvChatTitle.text = response.body()?.username ?: "Чат с пользователем $otherUserId"
                } else {
                    Log.e("MessageActivity", "Ошибка загрузки имени: ${response.code()}")
                    binding.tvChatTitle.text = "Чат с пользователем $otherUserId"
                }
            } catch (e: Exception) {
                Log.e("MessageActivity", "Ошибка сети: ${e.message}")
                binding.tvChatTitle.text = "Чат с пользователем $otherUserId"
            }
        }
    }

    private fun loadMessages() {
        if (otherUserId == 0) return
        lifecycleScope.launch {
            try {
                val currentUserId = sessionManager.getUserId() ?: return@launch
                val allMessages = mutableListOf<MessageResponse>()
                var page = 1
                var hasNext = true

                while (hasNext) {
                    val response = messagesRepository.getMessagesBetween(currentUserId, otherUserId, page)
                    if (response.isSuccessful) {
                        val paginatedResponse = response.body()
                        val serverMessages = paginatedResponse?.results.orEmpty()
                        allMessages.addAll(serverMessages)
                        messageIds.addAll(serverMessages.map { it.id })
                        hasNext = paginatedResponse?.next != null
                        page++
                        Log.d("MessageActivity", "Загружено ${serverMessages.size} сообщений со страницы ${page - 1}")
                    } else {
                        Log.e("MessageActivity", "Ошибка загрузки сообщений на странице $page: ${response.code()}")
                        break
                    }
                }

                // Передаём полный список в адаптер
                adapter.submitList(allMessages.toList()) {
                    binding.rvChat.post {
                        binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                        Log.d("MessageActivity", "Всего загружено ${adapter.itemCount} сообщений")
                    }
                }
                markMessagesAsRead(allMessages)
            } catch (e: Exception) {
                Log.e("MessageActivity", "Ошибка сети: ${e.message}")
            }
        }
    }

    private fun parseMessageResponse(json: JSONObject): MessageResponse {
        return MessageResponse(
            id = json.getInt("id"),
            fromUser = parseUserResponse(json.getJSONObject("sender")),
            toUser = json.getInt("receiver"),
            content = json.getString("content"),
            sentAt = json.getString("timestamp"),
            readAt = json.optString("read_at", null)
        )
    }

    private fun parseUserResponse(json: JSONObject): UserResponse {
        return UserResponse(
            id = json.getInt("id"),
            username = json.getString("username"),
            email = json.optString("email", null),
            avatar = json.optString("avatar", null),
            isEmailConfirmed = json.optBoolean("is_email_confirmed", false),
            role = json.optString("role", null),
            bio = json.optString("bio", null),
            created_at = json.optString("created_at", null),
            updated_at = json.optString("updated_at", null)
        )
    }

    private fun sendMessage(content: String) {
        val success = WebSocketManager.sendMessage(content, otherUserId)
        if (!success) {
            Log.e("MessageActivity", "Не удалось отправить сообщение")
        } else {
            val currentUserId = sessionManager.getUserId() ?: return
            val tempMessage = MessageResponse(
                id = -1,
                fromUser = UserResponse(
                    currentUserId,
                    sessionManager.getUserProfile()?.username ?: "Me",
                    null, null, false, null, null, null, null
                ),
                toUser = otherUserId,
                content = content,
                sentAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()).format(Date()),
                readAt = null
            )
            adapter.addMessage(tempMessage) {
                binding.rvChat.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private fun markMessageAsRead(message: MessageResponse) {
        lifecycleScope.launch {
            try {
                val response = messagesRepository.markMessageRead(message.id)
                if (!response.isSuccessful) {
                    Log.e("MessageActivity", "Ошибка отметки прочитанным: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MessageActivity", "Ошибка сети: ${e.message}")
            }
        }
    }

    private fun markMessagesAsRead(messages: List<MessageResponse>) {
        lifecycleScope.launch {
            messages.filter { it.toUser == sessionManager.getUserId() && it.readAt == null }
                .forEach { markMessageAsRead(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.removeMessageListener(messageListener)
        ChatSession.currentChatUserId = null
    }

    private fun finishWithResult() {
        setResult(RESULT_OK, Intent().putExtra("NEW_MESSAGE_SENT", true))
        super.finish()
    }

    override fun finish() {
        finishWithResult()
    }
}