package com.example.olimp.ui.events

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.olimp.data.models.CreateEventRequest
import com.example.olimp.data.models.Event
import com.example.olimp.data.models.EventPhotoResponse
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.network.ApiResponse
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
        loadEventsJob?.cancel()
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

    fun createEvent(request: CreateEventRequest, onSuccess: (Event) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val response = repository.createEvent(request)) {
                    is ApiResponse.Success -> {
                        onSuccess(response.data)
                    }
                    is ApiResponse.Error -> {
                        _error.value = "Ошибка ${response.code}: ${response.message}"
                    }
                    is ApiResponse.NetworkError -> {
                        _error.value = "Ошибка сети. Проверьте подключение."
                    }
                }
            } catch (e: Exception) {
                _error.value = "Неизвестная ошибка: ${e.message}"
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

    fun updateEventPreview(eventId: Int, part: MultipartBody.Part, onSuccess: (Event) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val response = repository.updateEventPreview(eventId, part)) {
                    is ApiResponse.Success -> {
                        onSuccess(response.data)
                    }
                    is ApiResponse.Error -> {
                        _error.value = "Ошибка загрузки превью: ${response.message ?: "Неизвестная ошибка"}"
                    }
                    is ApiResponse.NetworkError -> {
                        _error.value = "Ошибка сети при загрузке превью"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки превью: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun registerForEvent(
        eventId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.registerForEvent(eventId)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val error = response.errorBody()?.string() ?: "Ошибка регистрации"
                    onError(error)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Неизвестная ошибка")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelParticipation(
        eventId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.cancelParticipation(eventId)
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    val error = response.errorBody()?.string() ?: "Ошибка при отмене участия"
                    onError(error)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Неизвестная ошибка")
            } finally {
                _isLoading.value = false
            }
        }
    }

    class EventsViewModelFactory(
        private val repository: EventsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return EventsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}