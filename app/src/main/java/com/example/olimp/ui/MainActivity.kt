package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.olimp.R
import com.example.olimp.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var emailText: TextView
    private lateinit var profileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Проверка, залогинен ли пользователь
        if (!sessionManager.isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        welcomeText = findViewById(R.id.welcomeText)
        emailText = findViewById(R.id.emailText)
        profileButton = findViewById(R.id.profileButton)
        logoutButton = findViewById(R.id.logoutButton)

        val username = sessionManager.getUsername()
        val email = sessionManager.getEmail()

        welcomeText.text = "Добро пожаловать, $username!"
        emailText.text = "Email: $email"

        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        logoutButton.setOnClickListener {
            sessionManager.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)  // Закрывает приложение, а не отправляет пользователя обратно на экран входа
    }
}
