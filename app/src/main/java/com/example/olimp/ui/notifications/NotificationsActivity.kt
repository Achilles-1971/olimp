package com.example.olimp.ui.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.data.models.NotificationModel
import com.example.olimp.databinding.ActivityNotificationsBinding
import com.example.olimp.network.ApiService
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.SettingsActivity
import com.example.olimp.ui.events.EventDetailActivity
import com.example.olimp.ui.events.MyEventsActivity
import com.example.olimp.ui.messages.MessageActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val apiService: ApiService by lazy { RetrofitInstance.getApi(this) }
    private val deletedNotificationIds = mutableSetOf<Int>() // Локально "удалённые" уведомления

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Загружаем сохранённые удалённые ID
        loadDeletedNotificationIds()

        setupRecyclerView()
        setupBackButton()
        setupSwipeRefresh()
        checkNotificationSettings()
    }

    private fun checkNotificationSettings() {
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isEnabledInApp = sharedPref.getBoolean("notifications_enabled", true)
        val isEnabledInSystem = NotificationManagerCompat.from(this).areNotificationsEnabled()

        when {
            !isEnabledInApp -> {
                showDisabledMessage("Уведомления отключены в настройках приложения")
                binding.btnOpenSettings.visibility = View.VISIBLE
                binding.btnOpenSettings.setOnClickListener {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            !isEnabledInSystem -> {
                showDisabledMessage("Уведомления отключены в настройках устройства")
                binding.btnOpenSettings.visibility = View.VISIBLE
                binding.btnOpenSettings.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    })
                }
            }
            else -> loadNotifications()
        }
    }

    private fun showDisabledMessage(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = message
        binding.rvNotifications.visibility = View.GONE
        binding.btnOpenSettings.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removedNotification = adapter.getItemAt(position)
                adapter.removeItem(position)
                Snackbar.make(binding.root, "Уведомление удалено", Snackbar.LENGTH_LONG)
                    .setAction("ОТМЕНИТЬ") {
                        adapter.restoreItem(removedNotification, position)
                        deletedNotificationIds.remove(removedNotification.id)
                        saveDeletedNotificationIds()
                    }
                    .addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event != DISMISS_EVENT_ACTION) {
                                deletedNotificationIds.add(removedNotification.id)
                                saveDeletedNotificationIds()
                                // Опционально: удаление с сервера
                                lifecycleScope.launch {
                                    try {
                                        val response = apiService.deleteNotification(removedNotification.id)
                                        if (!response.isSuccessful) {
                                            Log.e("NotificationsActivity", "Ошибка удаления: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NotificationsActivity", "Ошибка сети: ${e.message}")
                                    }
                                }
                            }
                        }
                    }).show()
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvNotifications)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener { loadNotifications() }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadNotifications() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvNotifications.visibility = View.GONE
        binding.btnOpenSettings.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = false
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = apiService.getNotifications()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
                if (response.isSuccessful) {
                    val notifications = response.body().orEmpty()
                        .filter { it.id !in deletedNotificationIds } // Фильтруем удалённые
                    Log.d("NotificationsActivity", "Загружено уведомлений: ${notifications.size} (после фильтрации)")
                    adapter.submitList(notifications.toList())
                    if (notifications.isNotEmpty()) {
                        binding.rvNotifications.visibility = View.VISIBLE
                        Log.d("NotificationsActivity", "RecyclerView показан с ${notifications.size} элементами")
                        binding.rvNotifications.post { binding.rvNotifications.scrollToPosition(0) }
                    } else {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "У вас пока нет уведомлений"
                    }
                } else {
                    Log.e("NotificationsActivity", "Ошибка загрузки: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(this@NotificationsActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
                Log.e("NotificationsActivity", "Ошибка сети: ${e.message}", e)
                Toast.makeText(this@NotificationsActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleNotificationClick(notification: NotificationModel) {
        when (notification.type) {
            "new_message" -> {
                val intent = Intent(this, MessageActivity::class.java).apply {
                    putExtra("USER_ID", notification.entityId)
                }
                startActivity(intent)
            }
            "event_comment", "event_comment_reply", "event_joined", "event_left" -> {
                val intent = Intent(this, EventDetailActivity::class.java).apply {
                    putExtra("EVENT_ID", notification.entityId)
                }
                startActivity(intent)
            }
            "event_submitted", "event_approved", "event_rejected" -> {
                val intent = Intent(this, MyEventsActivity::class.java)
                startActivity(intent)
            }
            else -> Toast.makeText(this, "Неизвестный тип уведомления: ${notification.type}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isEnabledInApp = sharedPref.getBoolean("notifications_enabled", true)
        val isEnabledInSystem = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (isEnabledInApp && isEnabledInSystem) loadNotifications() // Загрузка только при входе
    }

    // Сохранение удалённых ID в SharedPreferences
    private fun saveDeletedNotificationIds() {
        val sharedPref = getSharedPreferences("notifications", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putStringSet("deleted_ids", deletedNotificationIds.map { it.toString() }.toSet())
            apply()
        }
    }

    // Загрузка удалённых ID из SharedPreferences
    private fun loadDeletedNotificationIds() {
        val sharedPref = getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val savedIds = sharedPref.getStringSet("deleted_ids", emptySet()) ?: emptySet()
        deletedNotificationIds.clear()
        deletedNotificationIds.addAll(savedIds.mapNotNull { it.toIntOrNull() })
    }
}