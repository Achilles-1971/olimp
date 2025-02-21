package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button

    companion object {
        private const val TAG = "RegisterActivity"
    }   

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Передаем ApiService в AuthRepository через RetrofitInstance, передавая контекст
        authRepository = AuthRepository(RetrofitInstance.createRetrofitInstance(this))

        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Валидация
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Вызов метода регистрации
            registerUser(username, email, password)
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        // Логируем входные данные
        Log.d(TAG, "registerUser() called with: username=$username, email=$email, password=$password")

        lifecycleScope.launch {
            try {
                // Вызываем метод репозитория
                val response = authRepository.registerUser(username, email, password)
                // Логируем код ответа
                Log.d(TAG, "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    // Логируем тело ответа
                    Log.d(TAG, "Response body (success): $body")

                    if (body != null) {
                        Toast.makeText(this@RegisterActivity, body.message, Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@RegisterActivity, VerifyEmailActivity::class.java)
                        intent.putExtra("USER_ID", body.user_id) // Передаем ID пользователя для подтверждения
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Ошибка сервера: пустой ответ", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Response body (error): $errorBody")
                    Toast.makeText(this@RegisterActivity, "Ошибка: $errorBody", Toast.LENGTH_SHORT).show()
                }

            } catch (e: IOException) {
                // Сетевые ошибки (отсутствие подключения или timeout)
                Log.e(TAG, "IOException: ${e.message}", e)
                Toast.makeText(this@RegisterActivity, "Ошибка сети. Проверьте подключение", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Любая другая ошибка
                Log.e(TAG, "Exception: ${e.message}", e)
                Toast.makeText(this@RegisterActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
