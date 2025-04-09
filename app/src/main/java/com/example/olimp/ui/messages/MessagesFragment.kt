package com.example.olimp.ui.messages

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
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
import com.example.olimp.data.repository.MessagesRepository
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.messages.adapter.ConversationAdapter
import com.example.olimp.utils.ChatSession
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConversationAdapter
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var repository: MessagesRepository

    companion object {
        private const val REQUEST_CODE_CHAT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = MessagesRepository(RetrofitInstance.getApi(requireContext()))
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

            // 🔒 Проверяем, открыт ли уже чат с этим пользователем
            if (ChatSession.currentChatUserId == userId) {
                // Чат уже открыт — ничего не делаем
                return@ConversationAdapter
            }

            val intent = Intent(requireContext(), MessageActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivityForResult(intent, REQUEST_CODE_CHAT)
        }
        recyclerView.adapter = adapter

        loadConversations()

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHAT && resultCode == RESULT_OK) {
            val newMessageSent = data?.getBooleanExtra("NEW_MESSAGE_SENT", false) ?: false
            if (newMessageSent) {
                loadConversations() // Обновляем только если было отправлено новое сообщение
            }
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
}
