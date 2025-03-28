package com.example.olimp.ui.events

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.olimp.data.models.CreateEventRequest
import com.example.olimp.data.models.Event
import com.example.olimp.data.models.EventPhotoResponse
import com.example.olimp.data.repository.EventsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class EventsViewModel(private val repository: EventsRepository) : ViewModel() {
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> get() = _events

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var loadEventsJob: Job? = null

    fun loadEvents(filter: String? = null) {
        Log.d("EventsViewModel", "🟢 loadEvents called with filter: $filter")
        loadEventsJob?.cancel() // Отменяем предыдущий запрос
        loadEventsJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("EventsViewModel", "🟢 Fetching events...")
                val result = repository.getEvents(filter)
                Log.d("EventsViewModel", "🟢 Events fetched: ${result.size} items")
                _events.value = result
            } catch (e: Exception) {
                Log.e("EventsViewModel", "🔴 Error fetching events: ${e.message}")
                _error.value = "Ошибка загрузки мероприятий: ${e.message}"
                _events.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Создание нового мероприятия
    fun createEvent(request: CreateEventRequest, onSuccess: (Event) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val createdEvent = repository.createEvent(request)
                if (createdEvent != null) {
                    onSuccess(createdEvent)
                } else {
                    _error.value = "Ошибка при создании мероприятия"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadEventPhotos(eventId: Int, parts: List<MultipartBody.Part>, onSuccess: (List<EventPhotoResponse>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uploadedPhotos = repository.uploadEventPhotos(eventId, parts)
                if (uploadedPhotos != null) {
                    onSuccess(uploadedPhotos)
                } else {
                    _error.value = "Ошибка при загрузке фотографий"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Удаление мероприятия
    fun deleteEvent(eventId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteEvent(eventId)
                if (success) {
                    onSuccess()
                } else {
                    _error.value = "Ошибка при удалении мероприятия"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}