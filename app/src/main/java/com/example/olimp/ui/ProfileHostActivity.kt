package com.example.olimp.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.olimp.R
import com.example.olimp.databinding.ActivityProfileHostBinding
import com.example.olimp.utils.SessionManager

class ProfileHostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем Toolbar с кнопкой "назад"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Получаем userId из Intent
        val userId = intent.getIntExtra("userId", 0)
        val currentUserId = SessionManager(this).getUserId() ?: 0

        // Загружаем нужный фрагмент в контейнер
        if (savedInstanceState == null) {
            val fragment = if (userId == currentUserId) {
                Log.d("ProfileHostActivity", "Loading ProfileFragment for current user: $userId")
                ProfileFragment()
            } else {
                Log.d("ProfileHostActivity", "Loading OtherProfileFragment for user: $userId")
                OtherProfileFragment().apply {
                    arguments = Bundle().apply {
                        putInt("userId", userId)
                    }
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}