package com.example.olimp.data.repository

import com.example.olimp.Mappers
import com.example.olimp.data.models.*
import com.example.olimp.network.ApiService
import retrofit2.Response

class CommentRepository(private val api: ApiService) {

    suspend fun getComments(
        entityType: String,
        entityId: Int,
        page: Int? = null
    ): Response<PaginatedCommentsResponse> {
        return api.getComments(entityType, entityId, page)
    }

    suspend fun createComment(request: CommentRequest): Response<Comment> {
        return api.createComment(request)
    }

    suspend fun updateComment(commentId: Int, request: CommentRequest): Response<Comment> {
        return api.updateComment(commentId, request)
    }

    suspend fun deleteComment(commentId: Int): Response<DeleteCommentResponse> {
        val response = api.deleteComment(commentId)
        return if (response.isSuccessful) {
            if (response.code() == 204) {
                Response.success(DeleteCommentResponse("Комментарий удалён"))
            } else if (response.body() != null) {
                val domainResponse = Mappers.mapDeleteCommentResponse(response.body()!!)
                Response.success(domainResponse)
            } else {
                Response.error(response.code(), response.errorBody()!!)
            }
        } else {
            Response.error(response.code(), response.errorBody()!!)
        }
    }

    suspend fun toggleCommentLike(commentId: Int): Response<CommentLikeToggleResponse> {
        return api.toggleCommentLike(commentId)
    }
}