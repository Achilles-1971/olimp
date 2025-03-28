package com.example.olimp.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.olimp.data.models.Event
import com.example.olimp.data.models.EventPhotoResponse
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.databinding.ActivityEventsDetailBinding
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class EventDetailFragment : Fragment() {

    private var _binding: ActivityEventsDetailBinding? = null
    private val binding get() = _binding!!

    private val eventsRepository by lazy {
        EventsRepository(RetrofitInstance.getApi(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityEventsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Настраиваем обработчики и загружаем данные мероприятия.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getInt("eventId") ?: 0
        if (eventId == 0) {
            Toast.makeText(requireContext(), "Не передан идентификатор мероприятия", Toast.LENGTH_SHORT).show()
            return
        }

        // Загрузка деталей мероприятия
        loadEventDetails(eventId)

        // Кнопка "Участвовать" / "Отменить участие"
        binding.btnParticipate.setOnClickListener {
            Toast.makeText(requireContext(), "Нажата кнопка для eventId=$eventId", Toast.LENGTH_SHORT).show()
            // TODO: Реализуйте вызов репозитория для регистрации / отмены участия
            // participateInEvent(eventId)
        }

        // Обработка нажатия на «Комментарии»
        binding.tvCommentsHeader.setOnClickListener {
            Toast.makeText(requireContext(), "Открыть/скрыть комментарии", Toast.LENGTH_SHORT).show()
            // TODO: Логика отображения или перехода к списку комментариев
        }
    }

    /**
     * Асинхронно загружаем данные мероприятия по eventId и обновляем UI.
     */
    private fun loadEventDetails(eventId: Int) {
        lifecycleScope.launch {
            val event: Event? = eventsRepository.getEventById(eventId)
            if (event == null) {
                Toast.makeText(requireContext(), "Ошибка загрузки мероприятия", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Заполняем заголовок
            binding.tvTitle.text = event.title

            // Заполняем описание
            binding.tvFullContent.text = event.description ?: "Описание отсутствует"

            // Подзаголовок (адрес / время / др.)
            if (!event.subheader.isNullOrEmpty()) {
                binding.tvSubheader.text = event.subheader
                binding.tvSubheader.visibility = View.VISIBLE
            } else {
                binding.tvSubheader.visibility = View.GONE
            }

            // Просмотры
            binding.tvViews.text = "Просмотры: ${event.viewsCount ?: 0}"

            // Участвует ли уже пользователь
            if (event.isRegistered == true) {
                binding.btnParticipate.text = "Отменить участие"
            } else {
                binding.btnParticipate.text = "Участвовать"
            }

            // Показываем фотографии (если есть)
            setupViewPagerPhotos(event.photos)
        }
    }

    /**
     * Настраиваем ViewPager2 для фотографий, если они есть.
     */
    private fun setupViewPagerPhotos(photos: List<EventPhotoResponse>?) {
        if (photos.isNullOrEmpty()) {
            // Если нет фото, скрываем ViewPager и индикатор
            binding.viewPagerPhotos.visibility = View.GONE
            binding.indicator.visibility = View.GONE
            return
        }

        // Показать ViewPager
        binding.viewPagerPhotos.visibility = View.VISIBLE
        binding.indicator.visibility = View.VISIBLE

        // Создаём адаптер
        val adapter = EventPhotoPagerAdapter(photos)
        binding.viewPagerPhotos.adapter = adapter

        // Подключаем индикатор к ViewPager
        binding.indicator.setViewPager(binding.viewPagerPhotos)
    }

    /**
     * Пример для регистрации (если нужно).
     */
    private fun participateInEvent(eventId: Int) {
        lifecycleScope.launch {
            // Пример вызова репозитория:
            // val result = eventsRepository.registerForEvent(eventId)
            // if (result.isSuccessful) { ... } else { ... }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
