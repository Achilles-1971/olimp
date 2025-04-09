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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var verificationCodeEditText: EditText
    private lateinit var verifyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        // Инициализируем репозиторий с передачей правильного экземпляра api
        authRepository = AuthRepository()


        verificationCodeEditText = findViewById(R.id.verificationCodeEditText)
        verifyButton = findViewById(R.id.verifyButton)

        val userId = intent.getIntExtra("USER_ID", -1)

        verifyButton.setOnClickListener {
            val code = verificationCodeEditText.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(this, "Введите код", Toast.LENGTH_SHORT).show()
            } else {
                verifyEmail(userId, code)
            }
        }
    }

    private fun verifyEmail(userId: Int, code: String) {
        lifecycleScope.launch {
            try {
                // Выполняем запрос в фоновом потоке
                val response = withContext(Dispatchers.IO) {
                    authRepository.verifyEmail(userId, code)
                }

                // Обрабатываем результат в главном потоке
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyEmailActivity, "Email подтверждён!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@VerifyEmailActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@VerifyEmailActivity, "Ошибка подтверждения: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VerifyEmailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
