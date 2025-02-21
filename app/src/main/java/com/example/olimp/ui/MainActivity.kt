package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.olimp.R
import com.example.olimp.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        // Проверяем, авторизован ли пользователь
        if (!sessionManager.isUserLoggedIn()) {
            // Если не авторизован, переходим на экран входа
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Проверяем, существует ли пользователь в базе данных
            val email = sessionManager.getUserEmail()
            if (email == null || !isUserValid(email)) {
                // Если пользователь не существует, очищаем сессию и отправляем на экран входа
                sessionManager.clearSession() // Исправленный метод
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Обработка оконных отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Функция для проверки валидности пользователя в базе данных
    private fun isUserValid(email: String): Boolean {
        // Здесь добавь логику проверки пользователя в базе данных
        // Например, запрос к API, чтобы убедиться, что пользователь существует
        return true // Возвращаем true, если пользователь существует
    }
}
