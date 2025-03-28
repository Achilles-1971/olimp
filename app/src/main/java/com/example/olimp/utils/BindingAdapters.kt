package com.example.olimp.utils

import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.olimp.R

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String?) {
    Log.d("BindingAdapter", "Original URL: $url")
    if (url.isNullOrEmpty()) {
        Log.d("BindingAdapter", "URL is null or empty, setting placeholder")
        view.setImageResource(R.drawable.ic_user_avatar)
        return
    }
    val fullUrl = if (url.startsWith("/media/")) "http://10.0.2.2:8000$url" else url
    Log.d("BindingAdapter", "Full URL: $fullUrl")
    Glide.with(view.context)
        .load(fullUrl)
        .placeholder(R.drawable.ic_user_avatar)
        .error(R.drawable.ic_user_avatar)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(view)
}
