package com.example.olimp.ui.friends

import FriendRequestsFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class FriendsPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FriendRequestsFragment()  // Заявки
            1 -> FriendsListFragment()     // Друзья
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}