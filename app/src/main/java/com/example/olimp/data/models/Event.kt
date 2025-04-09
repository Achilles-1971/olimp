package com.example.olimp.data.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

data class Event(
    val id: Int,
    val title: String,
    val subheader: String?,
    val description: String?,
    @SerializedName("start_datetime")
    val startDatetime: String?,
    @SerializedName("end_datetime")
    val endDatetime: String?,
    @JsonAdapter(OrganizerAdapter::class)
    val organizer: Organizer?,
    @SerializedName("views_count")
    val viewsCount: Int?,
    @SerializedName("comments_count")
    val commentsCount: Int?,
    @SerializedName("created_at")
    val createdAt: String?,
    val image: String?,
    val status: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("registrations_count")
    val registrationsCount: Int?,
    @SerializedName("is_registered")
    val isRegistered: Boolean?,
    val photos: List<EventPhotoResponse>?,
    @SerializedName("max_participants")
    val maxParticipants: Int?
)

data class Organizer(
    val id: Int?,
    val username: String?,
    val email: String? = null,
    val role: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
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

class OrganizerAdapter : JsonDeserializer<Organizer> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Organizer {
        return if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            Organizer.fromId(json.asInt)
        } else {
            context.deserialize(json, Organizer::class.java)
        }
    }
}

data class CreateEventRequest(
    @SerializedName("title") val title: String,
    @SerializedName("subheader") val subheader: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("start_datetime") val startDatetime: String,
    @SerializedName("end_datetime") val endDatetime: String? = null,
    @SerializedName("organizer") val organizer: Int,
    @SerializedName("address") val address: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("max_participants") val maxParticipants: Int? = 0
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

data class EventRegistrationRequest(
    val event: Int,
    val status: String = "going"
)

data class EventRegistrationResponse(
    val id: Int,
    @SerializedName("event")
    val eventId: Int,
    @SerializedName("user")
    val userId: Int,
    val status: String,
    @SerializedName("registered_at")
    val registeredAt: String
)
