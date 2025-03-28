package com.example.olimp.ui

import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.olimp.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchNotifications = findViewById<Switch>(R.id.switchNotifications)
        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "Уведомления включены" else "Уведомления отключены", Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, if (isChecked) "Тёмная тема включена" else "Тёмная тема отключена", Toast.LENGTH_SHORT).show()
        }
    }
}
