package com.example.olimp.ui.notifications

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.R
import com.example.olimp.data.models.NotificationModel
import com.example.olimp.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onItemClick: (NotificationModel) -> Unit
) : ListAdapter<NotificationModel, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {

    fun getItemAt(position: Int): NotificationModel = getItem(position)
    fun removeItem(position: Int) {
        val newList = currentList.toMutableList()
        newList.removeAt(position)
        submitList(newList)
        Log.d("NotificationAdapter", "Removed item at position $position, new size: ${newList.size}")
    }
    fun restoreItem(item: NotificationModel, position: Int) {
        val newList = currentList.toMutableList()
        newList.add(position, item)
        submitList(newList)
        Log.d("NotificationAdapter", "Restored item at position $position, new size: ${newList.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationBinding.inflate(inflater, parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        Log.d("NotificationAdapter", "Binding item at position: $position")
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationModel) {
            Log.d("NotificationAdapter", "Binding notification: ${notification.message}")
            binding.tvNotificationBody.text = notification.message
            binding.tvNotificationDate.text = formatDate(notification.createdAt)
            Log.d("NotificationAdapter", "TextView updated: body=${binding.tvNotificationBody.text}, date=${binding.tvNotificationDate.text}")

            val iconRes = when (notification.type) {
                "new_message" -> R.drawable.ic_send2
                "event_comment" -> R.drawable.ic_messages
                "event_comment_reply" -> R.drawable.ic_reply
                "event_comment_like" -> R.drawable.ic_like
                "event_approved" -> R.drawable.ic_add
                "event_rejected" -> R.drawable.ic_close
                "event_joined" -> R.drawable.ic_join
                "event_left" -> R.drawable.ic_leave
                "event_submitted" -> R.drawable.ic_notifications
                else -> R.drawable.ic_notifications
            }

            binding.ivNotificationIcon.setImageDrawable(
                ContextCompat.getDrawable(binding.root.context, iconRes)
            )

            binding.root.setOnClickListener {
                onItemClick(notification)
            }
        }

        private fun formatDate(dateStr: String): String {
            return try {
                val correctedDateStr = dateStr.replace(Regex("\\.(\\d{3})\\d+"), ".$1")
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(correctedDateStr)
                val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                formatter.format(date ?: Date())
            } catch (e: Exception) {
                Log.e("NotificationAdapter", "Date parsing failed: ${e.message}")
                dateStr
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
        override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
            return oldItem == newItem
        }
    }
}