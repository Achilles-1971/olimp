package com.example.olimp.data.repository

import com.example.olimp.data.models.MessageRequest
import com.example.olimp.data.models.MessageResponse
import com.example.olimp.data.models.ConversationResponse
import com.example.olimp.data.models.PaginatedMessagesResponse
import com.example.olimp.network.ApiService
import retrofit2.Response

class MessagesRepository(private val apiService: ApiService) {

    // Получение списка чатов
    suspend fun getConversations(): Response<List<ConversationResponse>> {
        return apiService.listConversations()
    }

    // Отправка сообщения
    suspend fun sendMessage(request: MessageRequest): Response<MessageResponse> {
        return apiService.sendMessage(request)
    }

    // Получение сообщений между двумя пользователями с поддержкой пагинации
    suspend fun getMessagesBetween(user1: Int, user2: Int, page: Int? = null): Response<PaginatedMessagesResponse> {
        return apiService.getMessagesBetween(user1, user2, page)
    }

    // Отметка сообщения как прочитанного
    suspend fun markMessageRead(messageId: Int): Response<MessageResponse> {
        return apiService.markMessageRead(messageId)
    }
}
