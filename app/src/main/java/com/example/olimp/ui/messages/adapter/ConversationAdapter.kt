package com.example.olimp.ui.messages.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.ConversationResponse
import com.example.olimp.databinding.ItemConversationBinding

class ConversationAdapter(
    private val onItemClick: (ConversationResponse) -> Unit
) : ListAdapter<ConversationResponse, ConversationAdapter.ConversationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val binding = ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConversationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConversationViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(conversation: ConversationResponse) {
            Log.d("ConversationAdapter", "Binding conversation: userId=${conversation.user.id}, lastMessage=${conversation.lastMessage.content}, unreadCount=${conversation.unreadCount}")

            binding.tvUsername.text = conversation.user.username
            binding.tvLastMessage.text = conversation.lastMessage.content

            val avatarUrl = if (!conversation.user.avatar.isNullOrEmpty() && conversation.user.avatar.startsWith("/media/")) {
                "http://10.0.2.2:8000" + conversation.user.avatar
            } else {
                conversation.user.avatar
            }
            Glide.with(binding.root.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_user_avatar)
                .error(R.drawable.ic_user_avatar)
                .into(binding.ivAvatar)

            if (conversation.unreadCount > 0) {
                binding.tvUnreadCount.text = conversation.unreadCount.toString()
                binding.tvUnreadCount.visibility = View.VISIBLE
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(conversation)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ConversationResponse>() {
        override fun areItemsTheSame(oldItem: ConversationResponse, newItem: ConversationResponse): Boolean {
            return oldItem.user.id == newItem.user.id // Уникальность по ID пользователя
        }

        override fun areContentsTheSame(oldItem: ConversationResponse, newItem: ConversationResponse): Boolean {
            return oldItem == newItem // Полное сравнение содержимого
        }
    }
}