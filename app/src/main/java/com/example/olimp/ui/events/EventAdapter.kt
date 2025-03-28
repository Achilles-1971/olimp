package com.example.olimp.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.Event
import com.example.olimp.databinding.ItemEventBinding

class EventAdapter(
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    private val items = mutableListOf<Event>()

    fun setItems(newEvents: List<Event>) {
        val diffResult = DiffUtil.calculateDiff(EventDiffCallback(items, newEvents))
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

    inner class EventViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.tvTitle.text = event.title
            binding.tvSubtitle.text = event.description ?: "Нет описания"
            // Отображаем количество участников, если оно есть
            binding.tvParticipants.text = if (event.registrationsCount != null) {
                "👥 Участников: ${event.registrationsCount}"
            } else {
                "👥 Участников: 0"
            }
            // Загрузка изображения события
            if (!event.image.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load("http://10.0.2.2:8000${event.image}")
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(binding.ivPreview)
            } else {
                Glide.with(binding.root.context)
                    .load(R.drawable.ic_placeholder)
                    .into(binding.ivPreview)
            }

            binding.root.setOnClickListener { onItemClick(event) }
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