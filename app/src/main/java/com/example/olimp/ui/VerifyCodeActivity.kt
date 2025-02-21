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

class VerifyCodeActivity : AppCompatActivity() {

    private lateinit var codeEditText: EditText
    private lateinit var verifyCodeButton: Button
    private lateinit var authRepository: AuthRepository
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        email = intent.getStringExtra("email")

        // Инициализируем репозиторий с передачей ApiService
        authRepository = AuthRepository(RetrofitInstance.createRetrofitInstance(this))

        codeEditText = findViewById(R.id.codeEditText)
        verifyCodeButton = findViewById(R.id.verifyCodeButton)

        verifyCodeButton.setOnClickListener {
            val code = codeEditText.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(this, "Введите код", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Вызываем метод для подтверждения кода
            verifyCode(email ?: "", code)
        }
    }

    private fun verifyCode(email: String, code: String) {
        lifecycleScope.launch {
            try {
                // Теперь используем verifyResetCode в репозитории
                val response = authRepository.verifyResetCode(email, code)
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyCodeActivity, "Код подтвержден", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@VerifyCodeActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@VerifyCodeActivity, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VerifyCodeActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
