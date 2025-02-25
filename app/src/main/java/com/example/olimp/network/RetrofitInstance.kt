package com.example.olimp.network

import android.content.Context
import com.example.olimp.utils.SessionManager
import com.example.olimp.MyApplication
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8000/"


    // Инициализация Retrofit с переданным Context для SessionManager
    fun createRetrofitInstance(context: Context): ApiService {
        val sessionManager = SessionManager(context)

        // Логирование запросов
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }

        // Интерцептор для добавления токена в заголовки
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
            val token = sessionManager.getAuthToken()

            if (!token.isNullOrEmpty()) {
                request.addHeader("Authorization", "Token $token")
            }

            chain.proceed(request.build())
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // Возвращаем объект ApiService с помощью Retrofit
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Доступ к api
    val api: ApiService by lazy {
        createRetrofitInstance(context = MyApplication.context)  // Передаем контекст приложения
    }
}
