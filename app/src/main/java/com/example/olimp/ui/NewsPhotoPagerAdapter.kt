package com.example.olimp.ui

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
            Glide.with(binding.ivPhoto.context)
                .load("http://10.0.2.2:8000${photo.photo}")
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
