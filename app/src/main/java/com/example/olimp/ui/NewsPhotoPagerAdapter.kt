package com.example.olimp.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.data.models.NewsPhoto
import com.example.olimp.databinding.ItemNewsPhotoBinding

class NewsPhotoPagerAdapter(private val photos: List<NewsPhoto>) :
    RecyclerView.Adapter<NewsPhotoPagerAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemNewsPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: NewsPhoto) {
            // Логируем URL для проверки
            Log.d("GlideURL", "Loading URL: ${photo.photo}")

            Glide.with(binding.ivPhoto.context)
                .load(photo.photo) // Используем photo.photo как есть, без добавления префикса
                .placeholder(com.example.olimp.R.drawable.ic_news)
                .into(binding.ivPhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemNewsPhotoBinding.inflate(
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
}