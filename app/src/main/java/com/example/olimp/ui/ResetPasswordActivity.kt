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
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var code1: EditText
    private lateinit var code2: EditText
    private lateinit var code3: EditText
    private lateinit var code4: EditText
    private lateinit var code5: EditText
    private lateinit var code6: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetPasswordButton: Button

    // Получаем email из Intent
    private var email: String? = null

    // Репозиторий для сброса пароля
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Получаем email, переданный из ForgotPasswordActivity
        email = intent.getStringExtra("email")

        initViews()

        authRepository = AuthRepository()

        resetPasswordButton.setOnClickListener {
            val resetCode = getResetCode() // Собираем код из всех полей
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (resetCode.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Заполните все поля")
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                showToast("Пароли не совпадают")
                return@setOnClickListener
            }

            confirmPasswordReset(email.orEmpty(), resetCode, newPassword)
        }
    }

    private fun initViews() {
        code1 = findViewById(R.id.code1)
        code2 = findViewById(R.id.code2)
        code3 = findViewById(R.id.code3)
        code4 = findViewById(R.id.code4)
        code5 = findViewById(R.id.code5)
        code6 = findViewById(R.id.code6)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
    }

    private fun getResetCode(): String {
        return code1.text.toString() +
                code2.text.toString() +
                code3.text.toString() +
                code4.text.toString() +
                code5.text.toString() +
                code6.text.toString()
    }

    private fun confirmPasswordReset(email: String, resetCode: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val response = authRepository.confirmPasswordReset(email, resetCode, newPassword)
                if (response.isSuccessful) {
                    showToast("Пароль изменён")
                    startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java))
                    finish()
                } else {
                    val errorText = response.errorBody()?.string()
                    showToast("Ошибка: $errorText")
                }
            } catch (e: Exception) {
                showToast("Ошибка: ${e.message}")
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
