package com.example.olimp.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.Friendship
import com.example.olimp.databinding.ItemFriendBinding
import com.example.olimp.utils.SessionManager

class FriendsAdapter(
    private val onAction1Click: (Friendship) -> Unit,
    private val onAction2Click: (Friendship) -> Unit,
    private val isRequests: Boolean // true для заявок, false для друзей
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private var friends: List<Friendship> = emptyList()

    fun submitList(newList: List<Friendship>) {
        friends = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    inner class FriendViewHolder(private val binding: ItemFriendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(friendship: Friendship) {
            val currentUserId = SessionManager(binding.root.context).getUserId()

            // Определяем, кто из пары — текущий пользователь, а кто его друг
            val friendUser = if (friendship.user.id == currentUserId) {
                friendship.friend
            } else {
                friendship.user
            }

            // Отображаем ник и аватар друга
            binding.tvUsername.text = friendUser.username

            // Обработка аватара как в EventDetailActivity
            friendUser.avatar?.let { avatarUrl ->
                val fullAvatarUrl = "http://10.0.2.2:8000$avatarUrl"
                Glide.with(binding.ivAvatar.context)
                    .load(fullAvatarUrl)
                    .placeholder(R.drawable.ic_profile)
                    .into(binding.ivAvatar)
            } ?: run {
                binding.ivAvatar.setImageResource(R.drawable.ic_profile)
            }

            // Настройка кнопок
            if (isRequests) {
                binding.btnAction1.text = "Принять"
                binding.btnAction2.text = "Отклонить"
                binding.btnAction1.setOnClickListener { onAction1Click(friendship) }
                binding.btnAction2.setOnClickListener { onAction2Click(friendship) }
            } else {
                binding.btnAction1.text = "Удалить"
                binding.btnAction2.text = "Написать"
                binding.btnAction1.setOnClickListener { onAction1Click(friendship) }
                binding.btnAction2.setOnClickListener { onAction2Click(friendship) }
            }
        }
    }
}
