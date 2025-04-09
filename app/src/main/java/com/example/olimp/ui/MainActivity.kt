package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.olimp.R
import com.example.olimp.data.models.UserResponse
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.databinding.ActivityMainBinding
import com.example.olimp.network.ApiService
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.events.EventsFragment
import com.example.olimp.ui.events.MyEventsActivity
import com.example.olimp.ui.friends.FindFriendsActivity
import com.example.olimp.ui.friends.FriendsActivity
import com.example.olimp.ui.notifications.NotificationsActivity
import com.example.olimp.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.olimp.ui.messages.MessagesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sessionManager = SessionManager(this)
        authRepository = AuthRepository()

        // 🔔 Запрашиваем разрешение на уведомления (Android 13+)
        requestNotificationPermissionIfNeeded()

        Log.d("MainActivity", "🟢 MainActivity onCreate started")

        lifecycleScope.launch {
            if (!sessionManager.isUserLoggedIn()) {
                Log.d("MainActivity", "🔴 User not logged in, redirecting to Login")
                redirectToLogin()
            } else {
                registerFcmTokenIfNeeded() // Проверяем и отправляем токен только если нужно
                val email = sessionManager.getUserEmail()
                if (email == null || !authRepository.isUserExists(email)) {
                    Log.d("MainActivity", "🔴 Email null or user not exists, clearing session")
                    sessionManager.clearSession()
                    redirectToLogin()
                } else {
                    initUI(savedInstanceState)
                    loadCurrentUserData()
                }
            }
        }
    }

    private fun registerFcmTokenIfNeeded() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val savedToken = sessionManager.getFcmToken()

                // Отправляем токен, если он новый или изменился
                if (savedToken != token) {
                    Log.d("FCM_TOKEN", "Токен устройства: $token")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val apiService: ApiService = RetrofitInstance.getApi(this@MainActivity)
                            val response = apiService.registerFcmToken(ApiService.FcmTokenRequest(token))
                            if (response.isSuccessful) {
                                val message = response.body()?.message
                                Log.d("FCM_TOKEN", "✅ Ответ сервера: $message")
                                sessionManager.saveFcmToken(token) // Сохраняем токен
                            } else {
                                Log.e("FCM_TOKEN", "❌ Ошибка отправки токена: ${response.code()} - ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e("FCM_TOKEN", "⚠️ Ошибка сети: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d("FCM_TOKEN", "ℹ️ Токен уже актуален: $token")
                }
            } else {
                Log.e("FCM_TOKEN", "❌ Ошибка получения токена: ${task.exception}")
            }
        }
    }

    private suspend fun loadCurrentUserData() = withContext(Dispatchers.IO) {
        try {
            val userId = sessionManager.getUserId() ?: return@withContext
            val userResponse = authRepository.getUserById(userId)
            if (userResponse.isSuccessful) {
                val user = userResponse.body()
                withContext(Dispatchers.Main) {
                    Log.d("MainActivity", "🟢 loadCurrentUserData success: user=${user?.username}, avatar=${user?.avatar}")
                    updateUI(user)
                }
            } else {
                Log.e("MainActivity", "🔴 Ошибка запроса: ${userResponse.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "🔴 Exception in loadCurrentUserData: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun updateUI(user: UserResponse?) {
        if (user == null) return
        binding.user = user

        val headerView = binding.navView.getHeaderView(0)
        val tvProfileNickname = headerView.findViewById<TextView>(R.id.tvProfileNickname)
        val ivProfileAvatar = headerView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivProfileAvatar)
        tvProfileNickname.text = user.username ?: "Username"

        val avatarUrl = if (!user.avatar.isNullOrEmpty() && user.avatar.startsWith("/media/")) {
            "http://10.0.2.2:8000" + user.avatar
        } else {
            user.avatar
        }
        Log.d("MainActivity", "🟢 Avatar URL: $avatarUrl")

        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_user_avatar)
                .error(R.drawable.ic_user_avatar)
                .into(ivProfileAvatar)
        } else {
            ivProfileAvatar.setImageResource(R.drawable.ic_user_avatar)
        }
    }

    private fun initUI(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "🟢 Initializing UI")

        if (savedInstanceState == null) {
            replaceFragment(EventsFragment())
            binding.bottomNav.selectedItemId = R.id.nav_events
            binding.tvTitle.text = "События"
            Log.d("MainActivity", "🟢 Initial fragment set: EventsFragment")
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_news -> {
                    binding.tvTitle.text = "Новости"
                    replaceFragment(NewsFragment())
                    Log.d("MainActivity", "🟢 Switched to NewsFragment")
                    true
                }
                R.id.nav_events -> {
                    binding.tvTitle.text = "События"
                    replaceFragment(EventsFragment())
                    Log.d("MainActivity", "🟢 Switched to EventsFragment")
                    true
                }
                R.id.nav_messages -> {
                    binding.tvTitle.text = "Сообщения"
                    replaceFragment(MessagesFragment())
                    Log.d("MainActivity", "🟢 Switched to MessagesFragment")
                    true
                }
                R.id.nav_profile -> {
                    binding.tvTitle.text = "Профиль"
                    replaceFragment(ProfileFragment())
                    Log.d("MainActivity", "🟢 Switched to ProfileFragment")
                    true
                }
                else -> false
            }
        }

        binding.ivAvatar.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
            Log.d("MainActivity", "🟢 Drawer opened")
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    binding.tvTitle.text = "Профиль"
                    replaceFragment(ProfileFragment())
                    binding.bottomNav.selectedItemId = R.id.nav_profile
                    Log.d("MainActivity", "🟢 Drawer: Switched to ProfileFragment")
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    Log.d("MainActivity", "🟢 Drawer: Started SettingsActivity")
                }
                R.id.nav_my_events -> {
                    startActivity(Intent(this, MyEventsActivity::class.java))
                    Log.d("MainActivity", "🟢 Drawer: Started MyEventsActivity")
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    Log.d("MainActivity", "🟢 Drawer: Started NotificationsActivity")
                }
                R.id.nav_friends -> {
                    startActivity(Intent(this, FriendsActivity::class.java))
                    Log.d("MainActivity", "🟢 Drawer: Started FriendsActivity")
                }
                R.id.nav_find_friends -> {
                    startActivity(Intent(this, FindFriendsActivity::class.java))
                    Log.d("MainActivity", "🟢 Drawer: Started FindFriendsActivity")
                }
                R.id.nav_logout -> {
                    sessionManager.clearSession()
                    redirectToLogin()
                    Log.d("MainActivity", "🟢 Drawer: Logged out")
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        Log.d("MainActivity", "🟢 Fragment replaced: ${fragment.javaClass.simpleName}")
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        Log.d("MainActivity", "🟢 Redirected to LoginActivity")
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("PERMISSION", "✅ Разрешение на уведомления получено")
        } else {
            Log.w("PERMISSION", "⚠️ Разрешение на уведомления отклонено пользователем")
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}