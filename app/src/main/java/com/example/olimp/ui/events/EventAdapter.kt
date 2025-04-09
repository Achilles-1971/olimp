package com.example.olimp.ui.events

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.Event
import com.example.olimp.databinding.ItemEventBinding
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventAdapter(
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val items = mutableListOf<Event>()
    private val timers = mutableMapOf<Int, CountDownTimer>()

    fun setItems(newEvents: List<Event>) {
        val diffResult = DiffUtil.calculateDiff(EventDiffCallback(items, newEvents))
        timers.forEach { it.value.cancel() }
        timers.clear()
        items.clear()
        items.addAll(newEvents)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    override fun onViewRecycled(holder: EventViewHolder) {
        super.onViewRecycled(holder)
        holder.stopTimer()
    }

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var timer: CountDownTimer? = null

        fun bind(event: Event) {
            // Заголовок и описание
            binding.tvTitle.text = event.title
            binding.tvSubtitle.text = event.subheader ?: binding.root.context.getString(R.string.no_description)

            // Изображение
            val baseUrl = "http://10.0.2.2:8000"
            if (!event.image.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load("$baseUrl${event.image}")
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(binding.ivPreview)
            } else {
                Glide.with(binding.root.context)
                    .load(R.drawable.ic_placeholder)
                    .into(binding.ivPreview)
            }

            // Прогресс участников с анимацией
            val registrations = event.registrationsCount ?: 0
            val maxParticipants = event.maxParticipants ?: 0
            if (maxParticipants > 0 && registrations <= maxParticipants) {
                val progress = (registrations * 100) / maxParticipants
                binding.progressParticipants.setProgressCompat(progress, true)
                binding.tvParticipantsCount.text = "$registrations / $maxParticipants"
            } else {
                binding.progressParticipants.setProgressCompat(0, true)
                binding.tvParticipantsCount.text = "$registrations"
            }

            // Прогресс времени с анимацией
            if (event.startDatetime != null && event.endDatetime != null) {
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val start = LocalDateTime.parse(event.startDatetime, formatter)
                val end = LocalDateTime.parse(event.endDatetime, formatter)
                val now = LocalDateTime.now()

                // Проверяем, идет ли мероприятие
                if (now.isBefore(end)) {
                    val totalDuration = Duration.between(start, end)
                    val elapsedDuration = Duration.between(start, now)
                    val totalMillis = totalDuration.toMillis()
                    val elapsedMillis = elapsedDuration.toMillis()

                    // Прогресс времени (от 0 до 100)
                    val timeProgress = if (totalMillis > 0) {
                        ((elapsedMillis * 100) / totalMillis).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }
                    binding.progressTime.setProgressCompat(timeProgress, true)

                    // Оставшееся время до конца
                    val remainingDuration = Duration.between(now, end)
                    if (remainingDuration.toMillis() > 0) {
                        val millisLeft = remainingDuration.toMillis()
                        timer?.cancel()
                        timer = object : CountDownTimer(millisLeft, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                                binding.tvTimeLeft.text = "${days}д ${hours}ч ${minutes}м"
                            }

                            override fun onFinish() {
                                binding.tvTimeLeft.text = binding.root.context.getString(R.string.event_finished)
                                binding.progressTime.setProgressCompat(100, true)
                            }
                        }.start()
                        timers[event.id] = timer!!
                    } else {
                        binding.tvTimeLeft.text = binding.root.context.getString(R.string.event_finished)
                        binding.progressTime.setProgressCompat(100, true)
                    }
                } else {
                    binding.tvTimeLeft.text = binding.root.context.getString(R.string.event_finished)
                    binding.progressTime.setProgressCompat(100, true)
                }
            } else {
                binding.tvTimeLeft.text = binding.root.context.getString(R.string.no_data)
                binding.progressTime.setProgressCompat(0, true)
            }

            // Обработчик клика
            binding.root.setOnClickListener { onItemClick(event) }
        }

        fun stopTimer() {
            timer?.cancel()
            timer = null
        }
    }

    private class EventDiffCallback(
        private val oldList: List<Event>,
        private val newList: List<Event>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}