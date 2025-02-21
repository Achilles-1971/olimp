package com.example.olimp

import android.app.Application
import android.content.Context

class MyApplication : Application() {

    // Статическое свойство для доступа к контексту приложения
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext  // Инициализация контекста
    }
}
