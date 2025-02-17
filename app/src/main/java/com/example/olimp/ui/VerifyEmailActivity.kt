package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.network.VerifyEmailRequest
import kotlinx.coroutines.launch

class VerifyEmailActivity : AppCompatActivity() {
    private lateinit var codeEdit: EditText
    private lateinit var confirmButton: Button
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_email)

        userId = intent.getIntExtra("USER_ID", 0)
        codeEdit = findViewById(R.id.codeEdit)
        confirmButton = findViewById(R.id.confirmButton)

        confirmButton.setOnClickListener {
            val code = codeEdit.text.toString()
            verifyCode(userId, code)
        }
    }

    private fun verifyCode(userId: Int, code: String) {
        lifecycleScope.launch {
            val response = RetrofitInstance.api.verifyEmail(VerifyEmailRequest(userId, code))
            if (response.isSuccessful) {
                Toast.makeText(this@VerifyEmailActivity, "E-mail подтверждён!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@VerifyEmailActivity, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this@VerifyEmailActivity, "Неверный код", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
