package com.example.olimp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.R
import com.example.olimp.data.models.Comment
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MessagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar

    // Используем собственную CoroutineScope
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Предполагаем, что newsId передаётся в аргументах фрагмента
    private var newsId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем newsId из аргументов, если он есть
        arguments?.let {
            newsId = it.getInt("newsId", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewComments)
        emptyView = view.findViewById(R.id.tvEmptyView)
        progressBar = view.findViewById(R.id.progressBar)

        recyclerView.layoutManager = LinearLayoutManager(context)
        commentAdapter = CommentAdapter(mutableListOf())
        recyclerView.adapter = commentAdapter

        loadComments()

        return view
    }

    private fun loadComments() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        coroutineScope.launch {
            try {
                // Вызов API для получения комментариев для новости с указанным ID
                val response = RetrofitInstance.getApi(requireContext()).getComments(entityType = "news", newsId = newsId)
                if (response.isSuccessful) {
                    // Извлекаем results из PaginatedCommentsResponse
                    val comments: List<Comment> = response.body()?.results ?: emptyList()
                    if (comments.isEmpty()) {
                        showEmptyView("Нет комментариев")
                    } else {
                        commentAdapter.updateData(comments)
                        recyclerView.visibility = View.VISIBLE
                    }
                } else {
                    showEmptyView("Ошибка загрузки комментариев: ${response.code()}")
                }
            } catch (e: Exception) {
                showEmptyView("Ошибка: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showEmptyView(message: String) {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}