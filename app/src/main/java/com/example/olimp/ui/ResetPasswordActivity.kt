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
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var resetCodeEditText: EditText
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

        authRepository = AuthRepository(RetrofitInstance.createRetrofitInstance(this))

        resetPasswordButton.setOnClickListener {
            val resetCode = resetCodeEditText.text.toString().trim()
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
        resetCodeEditText = findViewById(R.id.resetCodeEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
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
