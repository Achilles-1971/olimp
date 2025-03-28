package com.example.olimp.data.repository

import android.content.Context
import com.example.olimp.Mappers
import com.example.olimp.data.models.*
import com.example.olimp.network.ApiService
import com.example.olimp.network.RetrofitInstance
import retrofit2.Response

class NewsRepository(private val context: Context) {

    private val api: ApiService = RetrofitInstance.getApi(context)

    suspend fun getNews(sort: String? = null): Response<List<News>> {
        return api.getNews(sort)
    }

    suspend fun getNewsDetail(newsId: Int): Response<News> {
        return api.getNewsDetail(newsId)
    }

    suspend fun addLike(newsId: Int): Response<LikeResponse> {
        return api.addLike(newsId)
    }

    // Обновлено: возвращаем ViewResponse из data.models (доменная модель)
    suspend fun addView(newsId: Int): Response<ViewResponse> {
        val response = api.addView(newsId)
        return if (response.isSuccessful && response.body() != null) {
            val domainResponse = Mappers.mapViewResponse(response.body()!!)
            Response.success(domainResponse)
        } else {
            Response.error(response.code(), response.errorBody()!!)
        }
    }

    suspend fun getUserById(userId: Int): Response<UserResponse> {
        return api.getUserById(userId)
    }
}
