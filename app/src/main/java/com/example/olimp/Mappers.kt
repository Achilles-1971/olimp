package com.example.olimp

import com.example.olimp.data.models.DeleteCommentResponse
import com.example.olimp.data.models.ViewResponse
import com.example.olimp.network.ViewResponse as NetworkViewResponse

/**
 * Объект для преобразования (маппинга) сетевых моделей в доменные модели.
 */
object Mappers {

    /**
     * Преобразует сетевую модель [NetworkViewResponse] в доменную модель [ViewResponse].
     *
     * @param networkResponse ответ от API, содержащий количество просмотров.
     * @return доменная модель с количеством просмотров.
     */
    fun mapViewResponse(networkResponse: NetworkViewResponse): ViewResponse {
        return ViewResponse(
            viewsCount = networkResponse.viewsCount
        )
    }

    /**
     * Преобразует Map с ответом об удалении комментария в доменную модель [DeleteCommentResponse].
     *
     * @param responseMap карта, где ключ "detail" содержит информацию об удалении.
     * @return доменная модель с деталями удаления комментария.
     */
    fun mapDeleteCommentResponse(responseMap: Map<String, String>): DeleteCommentResponse {
        return DeleteCommentResponse(
            message = responseMap["detail"] ?: ""
        )
    }
}
