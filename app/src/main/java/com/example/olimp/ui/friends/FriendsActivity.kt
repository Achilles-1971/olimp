package com.example.olimp.ui.friends

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.olimp.R
import com.example.olimp.databinding.ActivityFriendsBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FriendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Назад
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Настройка ViewPager и TabLayout
        val adapter = FriendsPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Заявки"
                1 -> "Друзья"
                else -> null
            }
        }.attach()
    }
}