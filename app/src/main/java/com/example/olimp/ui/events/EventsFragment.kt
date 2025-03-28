package com.example.olimp.ui.events

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.olimp.R
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.network.RetrofitInstance
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton

class EventsFragment : Fragment() {

    private lateinit var viewModel: EventsViewModel
    private lateinit var adapter: EventAdapter
    private var lastToastTime: Long = 0
    private val toastInterval: Long = 2000 // 2 секунды
    private var currentFilter: String = "popular"
    private var emptyListMessage: TextView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("EventsFragment", "🟢 onAttach called")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EventsFragment", "🟢 onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("EventsFragment", "🟢 onCreateView called")

        val view = inflater.inflate(R.layout.fragment_events, container, false)

        // Инициализация RecyclerView
        val rvEvents = view.findViewById<RecyclerView>(R.id.rvEvents)
        if (rvEvents == null) {
            Log.e("EventsFragment", "🔴 RecyclerView not found!")
        } else {
            adapter = EventAdapter { event ->
                Log.d("EventsFragment", "🟢 Event clicked: ${event.title}")
                // Открываем EventDetailActivity, передавая ID мероприятия
                val intent = Intent(requireContext(), EventDetailActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                startActivity(intent)
            }
            rvEvents.layoutManager = LinearLayoutManager(requireContext())
            rvEvents.adapter = adapter
            adapter.setItems(emptyList()) // Пустой список по умолчанию
            Log.d("EventsFragment", "🟢 RecyclerView initialized")
        }

        // Инициализируем ViewModel
        try {
            val factory = EventsViewModelFactory(
                EventsRepository(RetrofitInstance.getApi(requireContext()))
            )
            viewModel = ViewModelProvider(this, factory)[EventsViewModel::class.java]
        } catch (e: Exception) {
            Log.e("EventsFragment", "🔴 Error initializing ViewModel: ${e.message}")
        }

        // Инициализация emptyListMessage
        emptyListMessage = view.findViewById(R.id.emptyListMessage)

        // Наблюдение за списком мероприятий
        viewModel.events.observe(viewLifecycleOwner) { events ->
            Log.d("EventsFragment", "🟢 Events updated: ${events.size} items")
            adapter.setItems(events)

            if (events.isEmpty()) {
                emptyListMessage?.visibility = View.VISIBLE
                emptyListMessage?.text = when (currentFilter) {
                    "upcoming" -> "Нет ближайших мероприятий"
                    "planned" -> "Нет планируемых мероприятий"
                    else -> "Нет популярных мероприятий"
                }
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastToastTime >= toastInterval) {
                    Toast.makeText(requireContext(), emptyListMessage?.text, Toast.LENGTH_SHORT)
                        .show()
                    lastToastTime = currentTime
                }
            } else {
                emptyListMessage?.visibility = View.GONE
            }
        }

        // Наблюдение за состоянием загрузки
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view.findViewById<ProgressBar>(R.id.progressBar)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE

            // Блокируем фильтры во время загрузки
            val chipPopular = view.findViewById<Chip>(R.id.chipPopular)
            val chipUpcoming = view.findViewById<Chip>(R.id.chipUpcoming)
            val chipPlanned = view.findViewById<Chip>(R.id.chipPlanned)
            chipPopular?.isEnabled = !isLoading
            chipUpcoming?.isEnabled = !isLoading
            chipPlanned?.isEnabled = !isLoading
        }

        // Наблюдение за ошибками
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastToastTime >= toastInterval) {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    lastToastTime = currentTime
                }
                Log.e("EventsFragment", "🔴 Error: $it")
            }
        }

        // SwipeRefreshLayout
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh?.setOnRefreshListener {
            Log.d("EventsFragment", "🟢 SwipeRefresh triggered")
            viewModel.loadEvents(currentFilter)
            swipeRefresh.isRefreshing = false
        }

        // Фильтры (Chip)
        view.findViewById<Chip>(R.id.chipPopular)?.setOnClickListener {
            Log.d("EventsFragment", "🟢 Chip Popular clicked")
            currentFilter = "popular"
            viewModel.loadEvents("popular")
            updateChipStates()
        }
        view.findViewById<Chip>(R.id.chipUpcoming)?.setOnClickListener {
            Log.d("EventsFragment", "🟢 Chip Upcoming clicked")
            currentFilter = "upcoming"
            viewModel.loadEvents("upcoming")
            updateChipStates()
        }
        view.findViewById<Chip>(R.id.chipPlanned)?.setOnClickListener {
            Log.d("EventsFragment", "🟢 Chip Planned clicked")
            currentFilter = "planned"
            viewModel.loadEvents("planned")
            updateChipStates()
        }

        // Начальная загрузка
        viewModel.loadEvents("popular")
        currentFilter = "popular"
        updateChipStates()

        // FAB (добавление нового мероприятия)
        val fabCreateEvent = view.findViewById<FloatingActionButton>(R.id.fabCreateEvent)
        fabCreateEvent?.setOnClickListener {
            Log.d("EventsFragment", "🟢 FAB clicked")
            Toast.makeText(requireContext(), "FAB НАЖАТ!", Toast.LENGTH_SHORT).show()
            openCreateEventScreen()
        }

        return view
    }

    private fun updateChipStates() {
        view?.findViewById<Chip>(R.id.chipPopular)?.isChecked = currentFilter == "popular"
        view?.findViewById<Chip>(R.id.chipUpcoming)?.isChecked = currentFilter == "upcoming"
        view?.findViewById<Chip>(R.id.chipPlanned)?.isChecked = currentFilter == "planned"
    }

    private fun openCreateEventScreen() {
        val intent = Intent(requireContext(), CreateEventActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emptyListMessage = null
    }
}
