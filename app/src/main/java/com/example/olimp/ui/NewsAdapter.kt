package com.example.olimp.ui

import android.content.Intent
import com.example.olimp.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.data.models.News
import com.example.olimp.databinding.ItemNewsBinding

class NewsAdapter : ListAdapter<News, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = getItem(position)
        holder.bind(newsItem)
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NewsDetailActivity::class.java)
            intent.putExtra("NEWS_ID", newsItem.id)
            intent.putExtra("NEWS_SUBHEADER", newsItem.subheader)  // Добавляем передачу подзаголовка
            context.startActivity(intent)
        }
    }

    inner class NewsViewHolder(private val binding: ItemNewsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: News) {
            binding.tvTitle.text = item.title
            binding.tvSubheader.text = item.subheader

            val baseUrl = "http://10.0.2.2:8000"
            val mediaPath = "/media/"
            val photoPath = item.photos?.firstOrNull()?.photo
            if (!photoPath.isNullOrEmpty()) {
                val fullUrl = if (photoPath.startsWith(mediaPath)) {
                    baseUrl + photoPath
                } else {
                    baseUrl + mediaPath + photoPath
                }
                Glide.with(binding.ivPhoto.context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_news)
                    .into(binding.ivPhoto)
            } else {
                binding.ivPhoto.setImageResource(R.drawable.ic_news)
            }
        }
    }
}

class NewsDiffCallback : DiffUtil.ItemCallback<News>() {
    override fun areItemsTheSame(oldItem: News, newItem: News) = (oldItem.id == newItem.id)
    override fun areContentsTheSame(oldItem: News, newItem: News) = (oldItem == newItem)
}
