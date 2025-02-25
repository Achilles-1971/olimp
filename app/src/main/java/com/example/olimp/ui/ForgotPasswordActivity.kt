package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var requestCodeButton: Button
    private lateinit var codeEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetPasswordButton: Button

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailEditText = findViewById(R.id.emailEditText)
        requestCodeButton = findViewById(R.id.requestCodeButton)
        codeEditText = findViewById(R.id.codeEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)

        // Первоначально скрываем поля для ввода кода и пароля (они заданы в XML как gone, но можно и так)
        codeEditText.visibility = View.GONE
        newPasswordEditText.visibility = View.GONE
        confirmPasswordEditText.visibility = View.GONE
        resetPasswordButton.visibility = View.GONE

        // Инициализация AuthRepository
        val apiService = RetrofitInstance.createRetrofitInstance(this)
        authRepository = AuthRepository(apiService)

        requestCodeButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Введите email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Запрос кода сброса пароля через API
            lifecycleScope.launch {
                val success = authRepository.requestPasswordReset(email)
                if (success) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Код сброса отправлен на email",
                        Toast.LENGTH_LONG
                    ).show()
                    // Отображаем скрытые поля
                    codeEditText.visibility = View.VISIBLE
                    newPasswordEditText.visibility = View.VISIBLE
                    confirmPasswordEditText.visibility = View.VISIBLE
                    resetPasswordButton.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Ошибка при отправке кода",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        resetPasswordButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val code = codeEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Отправка запроса на сброс пароля
            lifecycleScope.launch {
                val response = authRepository.confirmPasswordReset(email, code, newPassword)
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Пароль успешно изменён",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Переход обратно на экран логина
                    val intent = Intent(this@ForgotPasswordActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Ошибка сброса пароля",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
