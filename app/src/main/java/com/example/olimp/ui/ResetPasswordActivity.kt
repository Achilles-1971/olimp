package com.example.olimp.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetPasswordButton: Button
    private var email: String? = null
    private var resetCode: String? = null

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        email = intent.getStringExtra("email")
        resetCode = intent.getStringExtra("reset_code")

        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)

        // Инициализируем репозиторий с API
        authRepository = AuthRepository(RetrofitInstance.createRetrofitInstance(this))

        resetPasswordButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Введите новый пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Вызываем метод для сброса пароля
            confirmPasswordReset(email ?: "", resetCode ?: "", newPassword)
        }
    }

    private fun confirmPasswordReset(email: String, resetCode: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val response = authRepository.confirmPasswordReset(email, resetCode, newPassword)
                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "Пароль изменен", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ResetPasswordActivity, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResetPasswordActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
