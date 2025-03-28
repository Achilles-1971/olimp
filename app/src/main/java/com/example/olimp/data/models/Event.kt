package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName

data class Event(
    val id: Int,
    val title: String,
    val subheader: String?, // Добавлено
    val description: String?,
    @SerializedName("start_datetime")
    val startDatetime: String?,
    @SerializedName("end_datetime")
    val endDatetime: String?,
    val organizer: Organizer?,
    @SerializedName("views_count")
    val viewsCount: Int?,
    @SerializedName("created_at")
    val createdAt: String?,
    val image: String?, // Добавлено
    val status: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("registrations_count") // Добавлено
    val registrationsCount: Int?,
    @SerializedName("is_registered") // Добавлено
    val isRegistered: Boolean?,
    val photos: List<EventPhotoResponse>? // Добавлено (опционально, если используешь в будущем)
)

data class Organizer(
    val id: Int?,
    val username: String?,
    val email: String? = null, // Добавлено для полной информации
    val role: String? = null,  // Добавлено для полной информации
    val avatar: String? = null, // Добавлено для полной информации
    val bio: String? = null,    // Добавлено для полной информации
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    companion object {
        fun fromId(id: Int): Organizer {
            return Organizer(id = id, username = null)
        }
    }
}

data class CreateEventRequest(
    val title: String,
    val description: String?,
    @SerializedName("start_datetime")
    val startDatetime: String,
    @SerializedName("end_datetime")
    val endDatetime: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class EventPhotoResponse(
    val id: Int,
    val photo: String,
    @SerializedName("uploaded_at")
    val uploadedAt: String
)

data class EventResponse(
    val id: Int,
    val title: String,
    val description: String,
    val location: String?,
    val date: String,
    val time: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("creator_id")
    val creatorId: Int?,
    val image: String?,
    val category: String?,
    val isPopular: Boolean = false
)