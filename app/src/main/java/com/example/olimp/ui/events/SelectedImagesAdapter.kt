package com.example.olimp.ui.events

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.databinding.ItemSelectedImageBinding

class SelectedImagesAdapter : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    private val uris = mutableListOf<Uri>()
    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var onItemClickListener: ((Uri, Int) -> Unit)? = null
    private var onDeleteClickListener: ((Int) -> Unit)? = null

    fun setData(newUris: List<Uri>) {
        uris.clear()
        uris.addAll(newUris)
        selectedPosition = if (uris.isNotEmpty()) 0 else RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getCurrentUris(): List<Uri> = uris

    fun getSelectedUri(): Uri? {
        return if (selectedPosition != RecyclerView.NO_POSITION) uris[selectedPosition] else null
    }

    fun setOnItemClickListener(listener: (Uri, Int) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (Int) -> Unit) {
        onDeleteClickListener = listener
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition in uris.indices && toPosition in uris.indices) {
            val movedItem = uris.removeAt(fromPosition)
            uris.add(toPosition, movedItem)
            notifyItemMoved(fromPosition, toPosition)

            // Обновляем индекс выделения
            when {
                selectedPosition == fromPosition -> selectedPosition = toPosition
                selectedPosition in (fromPosition + 1)..toPosition -> selectedPosition--
                selectedPosition in toPosition until fromPosition -> selectedPosition++
            }
        }
    }

    fun removeItem(position: Int) {
        if (position in uris.indices) {
            uris.removeAt(position)
            notifyItemRemoved(position)

            // Обновляем selectedPosition, если нужно
            when {
                selectedPosition == position -> selectedPosition = RecyclerView.NO_POSITION
                selectedPosition > position -> selectedPosition--
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSelectedImageBinding.inflate(inflater, parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = uris[position]
        holder.bind(uri, position == selectedPosition)

        holder.itemView.setOnClickListener {
            val clickedPosition = holder.adapterPosition
            if (clickedPosition != RecyclerView.NO_POSITION) {
                val previous = selectedPosition
                selectedPosition = clickedPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onItemClickListener?.invoke(uri, clickedPosition)
            }
        }

        holder.binding.btnDeleteImage.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClickListener?.invoke(pos)
            }
        }
    }

    override fun getItemCount(): Int = uris.size

    class ImageViewHolder(val binding: ItemSelectedImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri, isSelected: Boolean) {
            Glide.with(binding.root.context)
                .load(uri)
                .into(binding.ivSelectedImage)

            binding.root.background = if (isSelected) {
                ContextCompat.getDrawable(binding.root.context, R.drawable.selected_border)
            } else null
        }
    }
    fun selectItem(position: Int) {
        if (position in uris.indices) {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(position)
        }
    }
}
