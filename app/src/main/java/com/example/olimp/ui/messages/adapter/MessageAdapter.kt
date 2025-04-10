package com.example.olimp.ui.messages.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.data.models.MessageResponse
import com.example.olimp.databinding.ItemMessageBinding
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: Int
) : ListAdapter<MessageResponse, MessageAdapter.MessageViewHolder>(DiffCallback()) {

    // Форматтеры для парсинга sentAt
    private val dateParsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()), // "2025-04-10T09:03:08.741627+00:00"
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())        // "2025-04-10T09:03:08+00:00"
    )
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Добавление нового сообщения
    fun addMessage(message: MessageResponse, onComplete: (() -> Unit)? = null) {
        val newList = currentList.toMutableList().apply {
            // Удаляем дубликаты или временные сообщения от того же отправителя
            removeAll { it.id == message.id || (it.id == -1 && message.id > 0 && it.fromUser.id == message.fromUser.id) }
            add(message)
            sortBy { parseSentAt(it.sentAt) } // Сортировка по времени как Long
        }
        submitList(newList) {
            Log.d("MessageAdapter", "Список обновлён: ${newList.map { "id=${it.id}, sentAt=${it.sentAt}" }}")
            onComplete?.invoke()
        }
    }

    // Замена временного сообщения на реальное
    fun replaceTemporaryMessage(realMessage: MessageResponse) {
        val newList = currentList.toMutableList()
        val tempIndex = newList.indexOfFirst {
            it.id == -1 &&
                    it.fromUser.id == realMessage.fromUser.id &&
                    it.toUser == realMessage.toUser &&
                    it.content == realMessage.content // Учитываем content для точной замены
        }
        if (tempIndex != -1) {
            newList[tempIndex] = realMessage
        } else {
            // Если временное сообщение не найдено, добавляем как новое
            newList.add(realMessage)
        }
        newList.sortBy { parseSentAt(it.sentAt) }
        submitList(newList)
    }

    // Удаление временных сообщений от пользователя
    fun removeTemporaryMessagesFromUser(userId: Int) {
        val newList = currentList.toMutableList().apply {
            removeAll { it.id == -1 && it.fromUser.id == userId }
        }
        submitList(newList)
    }

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
            binding.tvMessage.setTextColor(
                if (message.readAt != null && message.fromUser.id == currentUserId) Color.GRAY else Color.BLACK
            )

            // Настройка выравнивания сообщений
            constraintSet.clone(binding.root)
            if (message.fromUser.id == currentUserId) {
                binding.tvMessage.setBackgroundResource(com.example.olimp.R.drawable.bg_my_message)
                constraintSet.clear(binding.tvMessage.id, ConstraintSet.START)
                constraintSet.connect(binding.tvMessage.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constraintSet.clear(binding.tvTimestamp.id, ConstraintSet.START)
                constraintSet.connect(binding.tvTimestamp.id, ConstraintSet.END, binding.tvMessage.id, ConstraintSet.END)
            } else {
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
                val time = parseSentAt(sentAt)
                timeFormatter.format(Date(time))
            } catch (e: Exception) {
                Log.e("MessageAdapter", "Ошибка форматирования sentAt: $sentAt", e)
                sentAt
            }
        }
    }

    // Парсинг sentAt в Long для сортировки
    private fun parseSentAt(sentAt: String): Long {
        for (parser in dateParsers) {
            try {
                return parser.parse(sentAt)?.time ?: 0L
            } catch (e: ParseException) {
                continue
            }
        }
        Log.e("MessageAdapter", "Не удалось распарсить sentAt: $sentAt")
        return 0L
    }

    class DiffCallback : DiffUtil.ItemCallback<MessageResponse>() {
        override fun areItemsTheSame(oldItem: MessageResponse, newItem: MessageResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MessageResponse, newItem: MessageResponse): Boolean {
            return oldItem.content == newItem.content &&
                    oldItem.sentAt == newItem.sentAt &&
                    oldItem.readAt == newItem.readAt &&
                    oldItem.fromUser.id == newItem.fromUser.id &&
                    oldItem.toUser == newItem.toUser
        }
    }
}