package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.olimp.R
import com.example.olimp.utils.SessionManager

class SplashActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Проверяем токен
        if (!sessionManager.getAuthToken().isNullOrEmpty()) {
            // Уже есть токен -> MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // Нет токена -> показываем welcome layout
            setContentView(R.layout.activity_splash)
            findViewById<Button>(R.id.loginButton).setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            findViewById<Button>(R.id.registerButton).setOnClickListener {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
        }
    }
}

