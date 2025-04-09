package com.example.olimp.ui.friends

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.Friendship
import com.example.olimp.data.models.UserResponse

class FindFriendsAdapter(
    private val currentUserId: Int,
    private var friendships: List<Friendship>,
    private val onAddClick: (UserResponse) -> Unit,
    private val onCancelClick: (UserResponse) -> Unit,
    private val onAcceptClick: (UserResponse) -> Unit,
    private val onDeclineClick: (UserResponse) -> Unit,
    private val onAvatarClick: (UserResponse) -> Unit // Новый callback для клика по аватарке
) : RecyclerView.Adapter<FindFriendsAdapter.UserViewHolder>() {

    private var users: List<UserResponse> = listOf()

    fun submitList(list: List<UserResponse>) {
        users = list
        notifyDataSetChanged()
    }

    fun updateFriendships(newFriendships: List<Friendship>) {
        friendships = newFriendships
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val btnAction: Button = itemView.findViewById(R.id.btnAction)
        private val btnDecline: Button = itemView.findViewById(R.id.btnDecline)

        fun bind(user: UserResponse) {
            tvUsername.text = user.username

            // Если avatar не начинается с "http", добавляем базовый URL
            val fullAvatarUrl = if (!user.avatar.isNullOrEmpty() && !user.avatar.startsWith("http")) {
                "http://10.0.2.2:8000" + user.avatar
            } else {
                user.avatar
            }

            Glide.with(itemView.context)
                .load(fullAvatarUrl)
                .placeholder(R.drawable.ic_user_avatar)
                .into(ivAvatar)

            // Обработка клика по аватарке
            ivAvatar.setOnClickListener { onAvatarClick(user) }

            val existing = friendships.find {
                (it.user.id == currentUserId && it.friend.id == user.id) ||
                        (it.friend.id == currentUserId && it.user.id == user.id)
            }

            if (existing != null) {
                when (existing.status) {
                    "pending" -> {
                        if (existing.user.id == currentUserId) {
                            // Ты отправил заявку
                            btnAction.text = "Заявка отправлена"
                            btnAction.visibility = View.VISIBLE
                            btnDecline.visibility = View.GONE
                            btnAction.isEnabled = true
                            btnAction.setOnClickListener { onCancelClick(user) }
                        } else {
                            // Тебе отправили заявку — показать две кнопки
                            btnAction.text = "Принять"
                            btnAction.visibility = View.VISIBLE
                            btnDecline.visibility = View.VISIBLE
                            btnAction.setOnClickListener { onAcceptClick(user) }
                            btnDecline.setOnClickListener { onDeclineClick(user) }
                        }
                    }
                    "accepted" -> {
                        btnAction.text = "Друзья"
                        btnAction.visibility = View.VISIBLE
                        btnDecline.visibility = View.GONE
                        btnAction.isEnabled = false
                    }
                    else -> {
                        btnAction.text = "Добавить"
                        btnAction.visibility = View.VISIBLE
                        btnDecline.visibility = View.GONE
                        btnAction.setOnClickListener { onAddClick(user) }
                    }
                }
            } else {
                btnAction.text = "Добавить"
                btnAction.visibility = View.VISIBLE
                btnDecline.visibility = View.GONE
                btnAction.setOnClickListener { onAddClick(user) }
            }
        }
    }
}
