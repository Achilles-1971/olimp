package com.example.olimp.data.repository

import android.util.Log
import com.example.olimp.data.models.*
import com.example.olimp.network.ApiService
import com.example.olimp.Mappers
import com.example.olimp.MyApplication.Companion.context
import com.example.olimp.network.ApiResponse
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import retrofit2.Response

class EventsRepository(
    private val apiService: ApiService
) {

    suspend fun getEvents(filter: String? = null): List<Event> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getEvents(filter)
                if (response.isSuccessful) {
                    val events = response.body() ?: emptyList()
                    Log.d("EventsRepository", "🟢 getEvents success: ${events.size} events")
                    events
                } else {
                    Log.e("EventsRepository", "🔴 getEvents failed: ${response.code()} ${response.message()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("EventsRepository", "🔴 getEvents error", e)
                emptyList()
            }
        }
    }

    suspend fun getEventById(eventId: Int): Event? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getEventDetail(eventId)
                if (response.isSuccessful) {
                    val event = response.body()
                    Log.d("EventsRepository", "🟢 getEventById success: $event")
                    event
                } else {
                    Log.e("EventsRepository", "🔴 getEventById failed: ${response.code()} ${response.message()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("EventsRepository", "🔴 getEventById error", e)
                null
            }
        }
    }

    suspend fun deleteEvent(eventId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteEvent(eventId)
                if (response.isSuccessful) {
                    Log.d("EventsRepository", "🟢 deleteEvent success")
                    true
                } else {
                    Log.e("EventsRepository", "🔴 deleteEvent failed: ${response.code()} ${response.message()}")
                    false
                }
            } catch (e: Exception) {
                Log.e("EventsRepository", "🔴 deleteEvent error", e)
                false
            }
        }
    }

    suspend fun uploadEventPhotos(eventId: Int, parts: List<MultipartBody.Part>): List<EventPhotoResponse>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.uploadEventPhotos(eventId, parts)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun createEvent(request: CreateEventRequest): ApiResponse<Event> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createEvent(request)
                if (response.isSuccessful) {
                    val event = response.body()
                    if (event != null) {
                        Log.d("EventsRepository", "🟢 createEvent success: $event")
                        ApiResponse.Success(event)
                    } else {
                        ApiResponse.Error(response.code(), "Response body is null")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    ApiResponse.Error(response.code(), errorBody)
                }
            } catch (e: Exception) {
                ApiResponse.NetworkError
            }
        }
    }

    suspend fun addView(eventId: Int): Response<ViewResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.addView(eventId)
                if (response.isSuccessful && response.body() != null) {
                    val domainResponse = Mappers.mapViewResponse(response.body()!!)
                    Response.success(domainResponse)
                } else {
                    Response.error(response.code(), response.errorBody()!!)
                }
            } catch (e: Exception) {
                Log.e("EventsRepository", "🔴 addView error", e)
                throw e
            }
        }
    }

    suspend fun registerForEvent(eventId: Int): Response<EventRegistrationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.registerForEvent(eventId)
                if (response.isSuccessful && response.body() != null) {
                    val networkResponse = response.body()!!
                    val domainResponse = EventRegistrationResponse(
                        id = networkResponse.id,
                        eventId = networkResponse.eventId,
                        userId = networkResponse.userId,
                        status = networkResponse.status,
                        registeredAt = networkResponse.registeredAt
                    )
                    Response.success(domainResponse)
                } else {
                    Response.error(response.code(), response.errorBody()!!)
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun cancelParticipation(eventId: Int): Response<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.cancelParticipation(eventId)
                response
            } catch (e: Exception) {
                Log.e("EventsRepository", "🔴 cancelParticipation error", e)
                throw e
            }
        }
    }

    suspend fun updateEventPreview(eventId: Int, imagePart: MultipartBody.Part): ApiResponse<Event> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateEventPreview(eventId, imagePart)
                if (response.isSuccessful) {
                    val event = response.body()
                    if (event != null) {
                        ApiResponse.Success(event)
                    } else {
                        ApiResponse.Error(response.code(), "Response body is null")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    ApiResponse.Error(response.code(), errorBody)
                }
            } catch (e: Exception) {
                ApiResponse.NetworkError
            }
        }
    }
}
