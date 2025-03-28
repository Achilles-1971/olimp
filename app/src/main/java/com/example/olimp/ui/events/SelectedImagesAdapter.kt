package com.example.olimp.ui.events

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.databinding.ItemSelectedImageBinding

class SelectedImagesAdapter : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    private val uris = mutableListOf<Uri>()

    /**
     * Заменяем список uri на новый и оповещаем адаптер
     */
    fun setData(newUris: List<Uri>) {
        uris.clear()
        uris.addAll(newUris)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSelectedImageBinding.inflate(inflater, parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = uris[position]
        holder.bind(uri)
    }

    override fun getItemCount(): Int = uris.size

    class ImageViewHolder(private val binding: ItemSelectedImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            // Загружаем картинку через Glide
            Glide.with(binding.root.context)
                .load(uri)
                .into(binding.ivSelectedImage)
        }
    }
}
