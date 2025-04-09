package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName

data class NotificationModel(
    val id: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("entity_type")
    val entityType: String,
    @SerializedName("entity_id")
    val entityId: Int,
    @SerializedName("created_at")
    val createdAt: String
)
