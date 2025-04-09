package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    @SerializedName("to_user_id") val toUserId: Int,  // Изменено с "to_user" на "to_user_id"
    @SerializedName("content") val content: String
)

data class MessageResponse(
    val id: Int,
    @SerializedName("from_user") val fromUser: UserResponse,
    @SerializedName("to_user") val toUser: Int,  // Изменено с UserResponse на Int
    val content: String,
    @SerializedName("sent_at") val sentAt: String,
    @SerializedName("read_at") val readAt: String?
)

data class PaginatedMessagesResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<MessageResponse>
)

data class ConversationResponse(
    val user: UserResponse,
    @SerializedName("last_message") val lastMessage: MessageResponse,
    @SerializedName("unread_count") val unreadCount: Int
)
