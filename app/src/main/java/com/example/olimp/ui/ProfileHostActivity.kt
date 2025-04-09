package com.example.olimp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.olimp.R
import com.example.olimp.databinding.ActivityProfileHostBinding

class ProfileHostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем Toolbar с кнопкой "назад"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Получаем userId другого пользователя (передан через Intent)
        val userId = intent.getIntExtra("userId", 0)

        // Загружаем OtherProfileFragment в контейнер
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OtherProfileFragment().apply {
                    arguments = Bundle().apply {
                        putInt("userId", userId)
                    }
                })
                .commit()
        }
    }
}
