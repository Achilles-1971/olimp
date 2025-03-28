package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.olimp.R
import com.example.olimp.data.models.UserResponse
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.databinding.ActivityMainBinding
import com.example.olimp.network.ApiService
import com.example.olimp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.olimp.ui.events.EventsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sessionManager = SessionManager(this)

        // Настраиваем Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        authRepository = AuthRepository()

        Log.d("MainActivity", "🟢 MainActivity onCreate started")

        lifecycleScope.launch {
            if (!sessionManager.isUserLoggedIn()) {
                Log.d("MainActivity", "🔴 User not logged in, redirecting to Login")
                redirectToLogin()
            } else {
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
        val ivProfileAvatar = headerView.findViewById<ShapeableImageView>(R.id.ivProfileAvatar)
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
            // Временно устанавливаем EventsFragment для проверки
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
}