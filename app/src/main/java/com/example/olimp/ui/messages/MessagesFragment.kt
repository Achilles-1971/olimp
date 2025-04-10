package com.example.olimp.ui.messages

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.R
import com.example.olimp.data.models.ConversationResponse
import com.example.olimp.data.models.MessageResponse
import com.example.olimp.data.models.UserResponse
import com.example.olimp.data.repository.MessagesRepository
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.messages.adapter.ConversationAdapter
import com.example.olimp.utils.ChatSession
import com.example.olimp.utils.SessionManager
import com.example.olimp.utils.WebSocketManager
import kotlinx.coroutines.launch
import org.json.JSONObject

class MessagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var repository: MessagesRepository
    private lateinit var sessionManager: SessionManager
    private val conversationIds = mutableSetOf<Int>() // Для защиты от дубликатов сообщений
    private val messageListener: (JSONObject) -> Unit = { messageJson ->
        val messageData = messageJson.getJSONObject("message_data")
        val message = parseMessageResponse(messageData)
        if (conversationIds.add(message.id)) { // Проверка на дубликаты
            updateConversation(message)
        }
    }

    companion object {
        private const val REQUEST_CODE_CHAT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MessagesRepository(RetrofitInstance.getApi(requireContext()))
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewComments)
        emptyView = view.findViewById(R.id.tvEmptyView)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ConversationAdapter { conversation ->
            val userId = conversation.user.id
            if (ChatSession.currentChatUserId == userId) return@ConversationAdapter
            val intent = Intent(requireContext(), MessageActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivityForResult(intent, REQUEST_CODE_CHAT)
        }
        recyclerView.adapter = adapter

        loadConversations()
        WebSocketManager.addMessageListener(messageListener) // Подписка на сообщения

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHAT && resultCode == RESULT_OK) {
            val newMessageSent = data?.getBooleanExtra("NEW_MESSAGE_SENT", false) ?: false
            if (newMessageSent) loadConversations()
        }
    }

    private fun loadConversations() {
        showLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = repository.getConversations()
                if (response.isSuccessful) {
                    val conversations = response.body().orEmpty()
                    if (conversations.isEmpty()) {
                        showEmpty("У вас пока нет сообщений")
                    } else {
                        conversationIds.clear() // Очищаем перед загрузкой
                        conversationIds.addAll(conversations.map { it.lastMessage.id }) // Инициализация ID
                        adapter.submitList(conversations)
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    }
                } else {
                    showEmpty("Ошибка: ${response.code()}")
                }
            } catch (e: Exception) {
                showEmpty("Ошибка: ${e.message}")
            } finally {
                showLoading(false)
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

    private fun updateConversation(message: MessageResponse) {
        val currentUserId = sessionManager.getUserId() ?: return
        val otherUserId = if (message.fromUser.id == currentUserId) message.toUser else message.fromUser.id
        val currentList = adapter.currentList.toMutableList()
        val existingConversation = currentList.find { it.user.id == otherUserId }

        if (existingConversation != null) {
            val updatedConversation = existingConversation.copy(
                lastMessage = message,
                unreadCount = if (message.toUser == currentUserId && message.readAt == null) {
                    existingConversation.unreadCount + 1
                } else {
                    existingConversation.unreadCount
                }
            )
            currentList[currentList.indexOf(existingConversation)] = updatedConversation
        } else {
            val newConversation = ConversationResponse(
                user = message.fromUser,
                lastMessage = message,
                unreadCount = if (message.toUser == currentUserId && message.readAt == null) 1 else 0
            )
            currentList.add(newConversation)
        }

        requireActivity().runOnUiThread {
            adapter.submitList(currentList.sortedByDescending { it.lastMessage.sentAt })
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.GONE
        }
    }

    private fun showEmpty(message: String) {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = message
    }

    override fun onDestroyView() {
        WebSocketManager.removeMessageListener(messageListener) // Отписка от слушателя
        super.onDestroyView()
    }
}