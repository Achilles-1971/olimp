package com.example.olimp.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.EventPhotoResponse
import com.example.olimp.databinding.ItemEventPhotoBinding

class EventPhotoPagerAdapter(private var photos: List<EventPhotoResponse>) :
    RecyclerView.Adapter<EventPhotoPagerAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemEventPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photoResp: EventPhotoResponse) {
            // Проверяем, начинается ли строка с http:
            val fullUrl = if (photoResp.photo.startsWith("http")) {
                photoResp.photo
            } else {
                "http://10.0.2.2:8000${photoResp.photo}"
            }

            Glide.with(binding.ivPhoto.context)
                .load(fullUrl)
                .placeholder(R.drawable.ic_placeholder) // ваша заглушка
                .error(R.drawable.ic_placeholder)
                .into(binding.ivPhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemEventPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    // Метод для обновления данных адаптера
    fun setData(newPhotos: List<EventPhotoResponse>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}
