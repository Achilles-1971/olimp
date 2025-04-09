package com.example.olimp.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.Event
import com.example.olimp.databinding.ItemMyEventBinding
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MyEventsAdapter(
    private val onDeleteClick: (Event) -> Unit,
    private val onDetailsClick: (Event) -> Unit
) : ListAdapter<Event, MyEventsAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemMyEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemMyEventBinding) : RecyclerView.ViewHolder(binding.root) {

        private val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        private val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

        fun bind(event: Event) {
            // Обработка статуса мероприятия
            when (event.status) {
                "pending" -> {
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_hourglass_yellow)
                    binding.tvStatus.text = "В ожидании..."
                    binding.statusLayout.background = ContextCompat.getDrawable(binding.root.context, R.drawable.status_background)
                }
                "approved" -> {
                    binding.ivStatusIcon.setImageResource(R.drawable.ic_check_yellow)
                    binding.tvStatus.text = "Активна"
                    binding.statusLayout.background = ContextCompat.getDrawable(binding.root.context, R.drawable.status_background)
                }
                else -> {
                    binding.ivStatusIcon.setImageResource(android.R.drawable.ic_menu_help)
                    binding.tvStatus.text = "Неизвестный статус"
                    binding.statusLayout.background = ContextCompat.getDrawable(binding.root.context, R.drawable.status_background)
                }
            }

            // Загрузка превью мероприятия
            Glide.with(binding.ivPreview.context)
                .load(event.image)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(binding.ivPreview)

            // Название мероприятия
            binding.tvTitle.text = event.title ?: "Без названия"

            // Форматирование дат проведения
            val startDate = event.startDatetime?.let {
                OffsetDateTime.parse(it, inputFormatter).format(outputFormatter)
            } ?: "Не указано"

            val endDate = event.endDatetime?.let {
                OffsetDateTime.parse(it, inputFormatter).format(outputFormatter)
            } ?: "Не указано"

            binding.tvDates.text = "$startDate - $endDate"

            // Расчёт прогресса мероприятия
            val now = System.currentTimeMillis()
            val startMillis = event.startDatetime?.let {
                OffsetDateTime.parse(it, inputFormatter).toInstant().toEpochMilli()
            } ?: 0

            val endMillis = event.endDatetime?.let {
                OffsetDateTime.parse(it, inputFormatter).toInstant().toEpochMilli()
            } ?: 0

            val progress = when {
                startMillis == 0L || endMillis == 0L -> 0
                now < startMillis -> 0
                now > endMillis -> 100
                else -> ((now - startMillis).toFloat() / (endMillis - startMillis) * 100).toInt()
            }

            binding.progressBar.progress = progress

            // Просмотры и комментарии
            binding.tvViews.text = event.viewsCount?.toString() ?: "0"
            binding.tvComments.text = event.commentsCount?.toString() ?: "0"

            // Обработка нажатий
            binding.btnDelete.setOnClickListener { onDeleteClick(event) }
            binding.btnDetails.setOnClickListener { onDetailsClick(event) }
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
    override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
}
