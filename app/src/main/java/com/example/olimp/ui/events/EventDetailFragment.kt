package com.example.olimp.ui.events

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
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

    private var eventId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityEventsDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventId = arguments?.getInt("eventId") ?: 0
        if (eventId == 0) {
            Toast.makeText(requireContext(), R.string.error_invalid_event_id, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnParticipate.setOnClickListener {
            toggleParticipation()
        }

        binding.tvCommentsHeader.setOnClickListener {
            Toast.makeText(requireContext(), "Открыть/скрыть комментарии", Toast.LENGTH_SHORT).show()
        }

        loadEventDetails()
    }

    private fun loadEventDetails() {
        lifecycleScope.launch {
            try {
                val event = eventsRepository.getEventById(eventId)
                if (event != null) {
                    updateUI(event)
                } else {
                    Toast.makeText(requireContext(), R.string.error_loading_event_details, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EventDetailFragment", "Ошибка загрузки события: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(event: Event) {
        binding.tvTitle.text = event.title
        binding.tvFullContent.text = event.description ?: getString(R.string.no_description)
        binding.tvSubheader.apply {
            text = event.subheader ?: ""
            visibility = if (event.subheader.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        binding.tvViews.text = getString(R.string.views_count, event.viewsCount ?: 0)

        val registered = event.isRegistered == true
        binding.btnParticipate.text = getString(
            if (registered) R.string.cancel_participation_button else R.string.participate_button
        )
        binding.btnParticipate.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(), if (registered) R.color.gray else R.color.gold
        )

        setupViewPagerPhotos(event.photos)
    }

    private fun toggleParticipation() {
        lifecycleScope.launch {
            try {
                binding.btnParticipate.isEnabled = false
                val event = eventsRepository.getEventById(eventId)
                if (event?.isRegistered == true) {
                    eventsRepository.cancelParticipation(eventId)
                    Toast.makeText(requireContext(), getString(R.string.cancelled_successfully), Toast.LENGTH_SHORT).show()
                } else {
                    eventsRepository.registerForEvent(eventId)
                    Toast.makeText(requireContext(), getString(R.string.registered_successfully), Toast.LENGTH_SHORT).show()
                }
                loadEventDetails()
            } catch (e: Exception) {
                Log.e("EventDetailFragment", "Ошибка при участии: ${e.message}", e)
                Toast.makeText(requireContext(), getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnParticipate.isEnabled = true
            }
        }
    }

    private fun setupViewPagerPhotos(photos: List<EventPhotoResponse>?) {
        if (photos.isNullOrEmpty()) {
            binding.viewPagerPhotos.visibility = View.GONE
            binding.indicator.visibility = View.GONE
        } else {
            binding.viewPagerPhotos.visibility = View.VISIBLE
            binding.indicator.visibility = View.VISIBLE
            binding.viewPagerPhotos.adapter = EventPhotoPagerAdapter(photos)
            binding.indicator.setViewPager(binding.viewPagerPhotos)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
