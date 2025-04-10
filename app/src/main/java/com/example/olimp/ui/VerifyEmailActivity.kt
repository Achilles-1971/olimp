package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyEmailActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    private lateinit var code1: EditText
    private lateinit var code2: EditText
    private lateinit var code3: EditText
    private lateinit var code4: EditText
    private lateinit var code5: EditText
    private lateinit var code6: EditText

    private lateinit var verifyButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        authRepository = AuthRepository()

        code1 = findViewById(R.id.code1)
        code2 = findViewById(R.id.code2)
        code3 = findViewById(R.id.code3)
        code4 = findViewById(R.id.code4)
        code5 = findViewById(R.id.code5)
        code6 = findViewById(R.id.code6)

        verifyButton = findViewById(R.id.verifyButton)

        setupCodeInputs()

        val userId = intent.getIntExtra("USER_ID", -1)

        verifyButton.setOnClickListener {
            val code = listOf(code1, code2, code3, code4, code5, code6).joinToString("") {
                it.text.toString().trim()
            }

            if (code.length != 6) {
                Toast.makeText(this, "Введите 6-значный код", Toast.LENGTH_SHORT).show()
            } else {
                verifyEmail(userId, code)
            }
        }
    }

    private fun setupCodeInputs() {
        val inputs = listOf(code1, code2, code3, code4, code5, code6)

        for (i in inputs.indices) {
            inputs[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < inputs.size - 1) {
                        inputs[i + 1].requestFocus()
                    } else if (s?.isEmpty() == true && i > 0) {
                        inputs[i - 1].requestFocus()
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun verifyEmail(userId: Int, code: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    authRepository.verifyEmail(userId, code)
                }

                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyEmailActivity, "Email подтверждён!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@VerifyEmailActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this@VerifyEmailActivity,
                        "Ошибка подтверждения: ${response.errorBody()?.string()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@VerifyEmailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
