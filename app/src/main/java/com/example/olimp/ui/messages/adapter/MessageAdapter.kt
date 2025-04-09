package com.example.olimp.ui.messages.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.data.models.MessageResponse
import com.example.olimp.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: Int
) : ListAdapter<MessageResponse, MessageAdapter.MessageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val constraintSet = ConstraintSet()

        fun bind(message: MessageResponse) {
            Log.d("MessageAdapter", "Binding message: id=${message.id}, fromUser=${message.fromUser.id}, toUser=${message.toUser}, content=${message.content}")

            binding.tvMessage.text = message.content
            binding.tvTimestamp.text = formatTimestamp(message.sentAt)

            // Настраиваем цвета в зависимости от прочтения
            binding.tvMessage.setTextColor(
                if (message.readAt != null && message.fromUser.id == currentUserId)
                    Color.GRAY else Color.BLACK
            )

            // Переопределяем ConstraintSet для выравнивания
            constraintSet.clone(binding.root)

            // Сообщение от текущего пользователя — справа
            if (message.fromUser.id == currentUserId) {
                binding.tvMessage.setBackgroundResource(com.example.olimp.R.drawable.bg_my_message)

                constraintSet.clear(binding.tvMessage.id, ConstraintSet.START)
                constraintSet.connect(binding.tvMessage.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

                constraintSet.clear(binding.tvTimestamp.id, ConstraintSet.START)
                constraintSet.connect(binding.tvTimestamp.id, ConstraintSet.END, binding.tvMessage.id, ConstraintSet.END)

            } else {
                // Сообщение от другого пользователя — слева
                binding.tvMessage.setBackgroundResource(com.example.olimp.R.drawable.bg_other_message)

                constraintSet.clear(binding.tvMessage.id, ConstraintSet.END)
                constraintSet.connect(binding.tvMessage.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)

                constraintSet.clear(binding.tvTimestamp.id, ConstraintSet.END)
                constraintSet.connect(binding.tvTimestamp.id, ConstraintSet.START, binding.tvMessage.id, ConstraintSet.START)
            }

            constraintSet.connect(binding.tvTimestamp.id, ConstraintSet.TOP, binding.tvMessage.id, ConstraintSet.BOTTOM, 4)
            constraintSet.applyTo(binding.root)
        }

        private fun formatTimestamp(sentAt: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
                val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(sentAt)
                outputFormat.format(date ?: return sentAt)
            } catch (e: Exception) {
                Log.e("MessageAdapter", "Ошибка парсинга даты: $sentAt", e)
                sentAt
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MessageResponse>() {
        override fun areItemsTheSame(oldItem: MessageResponse, newItem: MessageResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MessageResponse, newItem: MessageResponse): Boolean {
            return oldItem == newItem
        }
    }
}
