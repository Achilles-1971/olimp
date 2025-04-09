package com.example.olimp.ui.events

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.data.models.Event
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.databinding.ActivityMyEventsBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyEventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyEventsBinding
    private lateinit var adapter: MyEventsAdapter
    private lateinit var sessionManager: SessionManager

    private val apiService by lazy { RetrofitInstance.getApi(this) }
    private val eventsRepository by lazy { EventsRepository(apiService) }

    private var events: List<Event> = emptyList()
    private var filter: String = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvHeaderTitle.text = "Мои мероприятия"

        binding.chipPending.setOnClickListener {
            filter = "pending"
            binding.chipPending.isChecked = true
            binding.chipActive.isChecked = false
            updateEvents()
        }

        binding.chipActive.setOnClickListener {
            filter = "approved"
            binding.chipActive.isChecked = true
            binding.chipPending.isChecked = false
            updateEvents()
        }

        adapter = MyEventsAdapter(
            onDeleteClick = { event -> confirmDeleteEvent(event) },
            onDetailsClick = { event -> openEventDetails(event) }
        )
        binding.rvMyEvents.layoutManager = LinearLayoutManager(this)
        binding.rvMyEvents.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadEvents()
        }

        loadEvents()
    }

    private fun loadEvents() {
        binding.progressBar.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false

        val token = sessionManager.getAuthToken()
        if (token == null) {
            Log.e("MyEventsActivity", "❌ Токен не найден")
            binding.progressBar.visibility = View.GONE
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.myEvents()
                if (response.isSuccessful) {
                    events = response.body().orEmpty()
                    Log.d("MyEventsActivity", "📦 Получено: ${events.size} мероприятий")
                } else {
                    Log.e("MyEventsActivity", "❌ Ошибка ответа: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MyEventsActivity", "⚠️ Ошибка загрузки мероприятий", e)
            } finally {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    updateEvents()
                }
            }
        }
    }

    private fun updateEvents() {
        val filteredEvents = events.filter { it.status == filter }
        runOnUiThread {
            adapter.submitList(filteredEvents)
            if (filteredEvents.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.swipeRefreshLayout.visibility = View.GONE
                binding.tvEmpty.text = when (filter) {
                    "approved" -> "Нет активных мероприятий"
                    "pending" -> "Нет мероприятий, ожидающих решения"
                    else -> "Нет мероприятий"
                }
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.swipeRefreshLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun confirmDeleteEvent(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Подтверждение удаления")
            .setMessage("Вы уверены, что хотите удалить мероприятие \"${event.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteEventFromServer(event)
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteEventFromServer(event: Event) {
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = eventsRepository.deleteEvent(event.id)
                if (success) {
                    Log.d("MyEventsActivity", "🖑️ Мероприятие удалено: ${event.title}")
                    events = events.filter { it.id != event.id }
                } else {
                    Log.e("MyEventsActivity", "❌ Ошибка удаления мероприятия")
                }
            } catch (e: Exception) {
                Log.e("MyEventsActivity", "⚠️ Ошибка при удалении мероприятия", e)
            } finally {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    updateEvents()
                }
            }
        }
    }

    private fun openEventDetails(event: Event) {
        val intent = Intent(this, EventDetailActivity::class.java).apply {
            putExtra("EVENT_ID", event.id)
        }
        startActivity(intent)
    }
}
