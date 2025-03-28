package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName

data class News(
    val id: Int,
    val title: String,
    val subheader: String?,
    @SerializedName("full_text")
    val fullText: String?,
    @SerializedName("views_count")
    val viewsCount: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val author: Int,
    val photos: List<NewsPhoto>? = emptyList(),
    val likes: Int = 0,
    @SerializedName("is_liked")
    val isLiked: Boolean = false
)

data class NewsPhoto(
    val id: Int,
    val news: Int,
    val photo: String,
    @SerializedName("uploaded_at")
    val uploadedAt: String
)
data class LikeResponse(
    val likes: Int,
    @SerializedName("is_liked")
    val isLiked: Boolean
)