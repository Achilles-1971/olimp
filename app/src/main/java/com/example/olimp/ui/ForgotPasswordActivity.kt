package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.network.ApiService
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var requestCodeButton: Button
    private lateinit var codeEditText: EditText
    private lateinit var confirmCodeButton: Button
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val api = RetrofitInstance.createRetrofitInstance(this)

        emailEditText = findViewById(R.id.emailEditText)
        requestCodeButton = findViewById(R.id.requestCodeButton)
        codeEditText = findViewById(R.id.codeEditText)
        confirmCodeButton = findViewById(R.id.confirmCodeButton)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)

        requestCodeButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendResetCode(api, email)
        }

        confirmCodeButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val code = codeEditText.text.toString().trim()

            if (email.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, "Введите email и код", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyResetCode(api, email, code)
        }

        resetPasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Используем актуальный метод для сброса пароля
            resetPasswordRequest(api, email, newPassword)
        }
    }

    private fun sendResetCode(api: ApiService, email: String) {
        lifecycleScope.launch {
            try {
                val response = api.requestPasswordReset(mapOf("email" to email))

                if (response.isSuccessful) {
                    Toast.makeText(this@ForgotPasswordActivity, "Код отправлен на email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyResetCode(api: ApiService, email: String, code: String) {
        lifecycleScope.launch {
            try {
                val response = api.verifyResetCode(mapOf("email" to email, "reset_code" to code))

                if (response.isSuccessful) {
                    Toast.makeText(this@ForgotPasswordActivity, "Код подтвержден. Введите новый пароль.", Toast.LENGTH_SHORT).show()

                    // Показываем поля для ввода нового пароля
                    newPasswordEditText.visibility = android.view.View.VISIBLE
                    confirmPasswordEditText.visibility = android.view.View.VISIBLE
                    resetPasswordButton.visibility = android.view.View.VISIBLE
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPasswordRequest(api: ApiService, email: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                // Теперь вызываем правильный метод для сброса пароля
                val response = api.confirmPasswordReset(mapOf("email" to email, "new_password" to newPassword))

                if (response.isSuccessful) {
                    Toast.makeText(this@ForgotPasswordActivity, "Пароль изменён", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForgotPasswordActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
