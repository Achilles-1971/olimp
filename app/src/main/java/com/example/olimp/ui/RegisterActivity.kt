package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.network.RegisterRequest
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var usernameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameEdit = findViewById(R.id.usernameEdit)
        emailEdit = findViewById(R.id.emailEdit)
        passwordEdit = findViewById(R.id.passwordEdit)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameEdit.text.toString()
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(username, email, password)
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        lifecycleScope.launch {
            val response = RetrofitInstance.api.registerUser(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                val body = response.body()
                Toast.makeText(this@RegisterActivity, body?.message ?: "Регистрация успешна", Toast.LENGTH_SHORT).show()

                val intent = Intent(this@RegisterActivity, VerifyEmailActivity::class.java)
                intent.putExtra("USER_ID", body?.user_id)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this@RegisterActivity, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
