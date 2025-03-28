package com.example.olimp.network

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.olimp.ui.LoginActivity
import com.example.olimp.utils.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.olimp.BuildConfig
import com.example.olimp.data.models.Organizer

object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8000/"

    private fun createOkHttpClient(context: Context): OkHttpClient {
        val sessionManager = SessionManager(context)
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = sessionManager.getAuthToken()
                if (!token.isNullOrEmpty()) {
                    Log.d("RetrofitInstance", "🟢 Токен добавлен к запросу: Token $token")
                    requestBuilder.addHeader("Authorization", "Token $token")
                } else {
                    Log.w("RetrofitInstance", "❌ Токен отсутствует для ${chain.request().url}")
                }
                requestBuilder.addHeader("Content-Type", "application/json")
                val request = requestBuilder.build()
                Log.d("RetrofitInstance", "📤 Полный запрос: ${request.method} ${request.url}")
                Log.d("RetrofitInstance", "📤 Заголовки: ${request.headers}")
                val response = chain.proceed(request)
                Log.d("RetrofitInstance", "📥 Ответ: ${response.code} ${response.message}")
                response
            }
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 401) {
                    Log.d("RetrofitInstance", "🚨 401 Unauthorized - Переход на экран логина")
                    sessionManager.clearAuthToken()
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
                response
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val gson = GsonBuilder()
        .serializeNulls()
        .setLenient()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(Organizer::class.java, OrganizerDeserializer()) // Добавляем десериализатор
        .create()

    private fun getRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApi(context: Context): ApiService {
        return getRetrofit(context).create(ApiService::class.java)
    }
}