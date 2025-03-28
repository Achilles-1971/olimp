package com.example.olimp.data.models

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("id")
    val id: Int,
    @SerializedName("user")
    val user: UserResponse?,
    @SerializedName("content")
    val content: String,
    @SerializedName("parent_comment_id")
    val parentCommentId: Int? = null,
    @SerializedName("likes_count")
    val likesCount: Int? = 0,
    @SerializedName("is_liked")
    val isLiked: Boolean? = false,
    @SerializedName("user_avatar")
    val userAvatar: String? = null,
    @SerializedName("children")
    val children: List<Comment> = emptyList(),
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("reply_to")
    val replyTo: String? = null
) {
    fun isRootComment(): Boolean = parentCommentId == null
}

data class CommentResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("entity_type")
    val entityType: String,
    @SerializedName("entity_id")
    val entityId: Int,
    @SerializedName("content")
    val content: String,
    @SerializedName("parent_comment_id")
    val parentCommentId: Int?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("user")
    val user: UserResponse?,
    @SerializedName("likes_count")
    val likesCount: Int? = 0
)

data class CommentRequest(
    @SerializedName("entity_id")
    val entityId: Int,
    @SerializedName("content")
    val content: String,
    @SerializedName("entity_type")
    val entityType: String,
    @SerializedName("parent_comment_id")
    val parentCommentId: Int? = null
)

data class FlatComment(
    @SerializedName("comment")
    val comment: Comment,
    @SerializedName("depth")
    val depth: Int,
    val isPlaceholder: Boolean = false,
    val hiddenCount: Int = 0,
    var isExpanded: Boolean = false
)

data class CommentLikeToggleResponse(
    val comment_id: Int,
    val message: String,
    val likes_count: Int,
    @SerializedName("liked") // Добавляем поле liked
    val liked: Boolean // Соответствует серверному ответу
)

data class PaginatedCommentsResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<Comment>
)

data class DeleteCommentResponse(
    @SerializedName("message")
    val message: String
)

data class ViewResponse(
    @SerializedName("views_count")
    val viewsCount: Int
)

// Это уже domain-класс, можно использовать, если ты отделяешь ViewModel от сети
data class DomainViewResponse(
    val viewsCount: Int
)
