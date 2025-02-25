package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Инициализация ApiService и репозитория
        val apiService = RetrofitInstance.createRetrofitInstance(this)
        authRepository = AuthRepository(apiService)
        sessionManager = SessionManager(this)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginUser(email, password)
        }

        // Переход на экран сброса пароля (ForgotPasswordActivity)
        forgotPasswordButton.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = authRepository.loginUser(email, password)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Сохраняем токен и email
                        sessionManager.saveAuthToken(body.token, email)
                        Toast.makeText(this@LoginActivity, "Вход выполнен", Toast.LENGTH_SHORT).show()
                        // Переход в MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Ошибка сервера", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@LoginActivity, "Ошибка: $errorBody", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
