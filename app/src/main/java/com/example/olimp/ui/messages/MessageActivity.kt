package com.example.olimp.ui.messages

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.data.models.MessageRequest
import com.example.olimp.data.models.MessageResponse
import com.example.olimp.data.repository.MessagesRepository
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.ActivityMessageBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.messages.adapter.MessageAdapter
import com.example.olimp.utils.ChatSession
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.launch

class MessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBinding
    private lateinit var messagesRepository: MessagesRepository
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: MessageAdapter
    private var otherUserId: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val pollingInterval = 5000L
    private val pollingRunnable = object : Runnable {
        override fun run() {
            loadMessages()
            handler.postDelayed(this, pollingInterval)
        }
    }

    private fun startPolling() {
        handler.post(pollingRunnable)
    }

    private fun stopPolling() {
        handler.removeCallbacks(pollingRunnable)
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
        } else {
            Log.d("MessageActivity", "onNewIntent: тот же пользователь, ничего не делаем")
        }
    }

    private fun updateOtherUserId(intent: Intent?) {
        val newUserId = intent?.getIntExtra("USER_ID", 0) ?: 0
        Log.d("MessageActivity", "Received USER_ID: $newUserId")
        if (newUserId == 0) {
            Log.e("MessageActivity", "otherUserId is 0, something went wrong with Intent")
        } else if (newUserId != otherUserId) {
            otherUserId = newUserId
            ChatSession.currentChatUserId = otherUserId
            Log.d("MessageActivity", "Updated otherUserId to: $otherUserId")
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(sessionManager.getUserId() ?: 0)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter
    }

    private fun setupInput() {
        binding.btnBack.setOnClickListener {
            finishWithResult()
        }

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
                    val user = response.body()
                    binding.tvChatTitle.text = user?.username ?: "Чат с пользователем $otherUserId"
                } else {
                    Log.e("MessageActivity", "Ошибка загрузки имени: ${response.code()}")
                    binding.tvChatTitle.text = "Чат с пользователем $otherUserId"
                }
            } catch (e: Exception) {
                Log.e("MessageActivity", "Ошибка сети при загрузке имени: ${e.message}")
                binding.tvChatTitle.text = "Чат с пользователем $otherUserId"
            }
        }
    }

    private fun loadMessages() {
        if (otherUserId == 0) {
            Log.e("MessageActivity", "Cannot load messages: otherUserId is 0")
            return
        }
        lifecycleScope.launch {
            try {
                val currentUserId = sessionManager.getUserId() ?: return@launch
                val response = messagesRepository.getMessagesBetween(currentUserId, otherUserId)
                if (response.isSuccessful) {
                    val messages = response.body()?.results.orEmpty()
                        .sortedBy { it.sentAt }

                    adapter.submitList(messages) {
                        binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                    }

                    markMessagesAsRead(messages)
                } else {
                    Log.e("MessageActivity", "Ошибка загрузки сообщений: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MessageActivity", "Ошибка сети при загрузке сообщений: ${e.message}")
            }
        }
    }

    private fun sendMessage(content: String) {
        if (otherUserId == 0) {
            Log.e("MessageActivity", "Cannot send message: otherUserId is 0")
            return
        }
        lifecycleScope.launch {
            try {
                val request = MessageRequest(toUserId = otherUserId, content = content)
                val response = messagesRepository.sendMessage(request)
                if (response.isSuccessful) {
                    val newMessage = response.body()
                    if (newMessage != null) {
                        val currentList = adapter.currentList.toMutableList()
                        currentList.add(newMessage)
                        val sortedList = currentList.sortedBy { it.sentAt }
                        adapter.submitList(sortedList) {
                            binding.rvChat.scrollToPosition(adapter.itemCount - 1)
                        }

                        setResult(RESULT_OK, Intent().putExtra("NEW_MESSAGE_SENT", true))
                    }
                } else {
                    Log.e("MessageActivity", "Ошибка отправки сообщения: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MessageActivity", "Ошибка сети при отправке: ${e.message}")
            }
        }
    }

    private fun markMessagesAsRead(messages: List<MessageResponse>) {
        lifecycleScope.launch {
            messages.filter { it.toUser == sessionManager.getUserId() && it.readAt == null }
                .forEach { message ->
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
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
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
