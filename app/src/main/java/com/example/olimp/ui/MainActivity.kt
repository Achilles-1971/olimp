package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.AuthRepository
import com.example.olimp.network.ApiService
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")  // Заменить на реальный URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        authRepository = AuthRepository(apiService)

        lifecycleScope.launch {
            if (!sessionManager.isUserLoggedIn()) {
                redirectToLogin()
            } else {
                val email = sessionManager.getUserEmail()
                if (email == null || !isUserValid(email)) {
                    sessionManager.clearSession()
                    redirectToLogin()
                }
            }
        }
    }

    private suspend fun isUserValid(email: String): Boolean {
        return authRepository.isUserExists(email)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
