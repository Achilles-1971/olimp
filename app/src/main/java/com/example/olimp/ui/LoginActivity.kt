package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.network.ApiService
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        val apiService = RetrofitInstance.getApi(this)
        authRepository = AuthRepository()
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

        // Переход на экран сброса пароля
        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = authRepository.loginUser(email, password)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Из body получаем user_id и token
                        val userId = body.user_id
                        val token = body.token

                        // Сохраняем токен, email и userId
                        sessionManager.saveAuthToken(token, email, userId)

                        // Регистрируем FCM-токен после входа
                        registerFcmToken()

                        Toast.makeText(this@LoginActivity, "Вход выполнен", Toast.LENGTH_SHORT).show()

                        // Переход в MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
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

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Токен устройства (LoginActivity): $token")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val apiService: ApiService = RetrofitInstance.getApi(this@LoginActivity)
                        val response = apiService.registerFcmToken(ApiService.FcmTokenRequest(token))
                        if (response.isSuccessful) {
                            val message = response.body()?.message
                            Log.d("FCM_TOKEN", "✅ Токен зарегистрирован: $message")
                            sessionManager.saveFcmToken(token) // Сохраняем токен
                        } else {
                            Log.e("FCM_TOKEN", "❌ Ошибка регистрации токена: ${response.code()} - ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("FCM_TOKEN", "⚠️ Ошибка сети: ${e.message}", e)
                    }
                }
            } else {
                Log.e("FCM_TOKEN", "❌ Ошибка получения токена: ${task.exception}")
            }
        }
    }
}