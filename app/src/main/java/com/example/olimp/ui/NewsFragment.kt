package com.example.olimp.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.olimp.R
import com.example.olimp.data.repository.NewsRepository
import com.example.olimp.databinding.FragmentNewsBinding
import kotlinx.coroutines.launch

class NewsFragment : Fragment(R.layout.fragment_news) {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    // Репозиторий для работы с новостями
    private val repository by lazy { NewsRepository(requireContext()) }

    // Адаптер для списка новостей
    private lateinit var newsAdapter: NewsAdapter

    // Текущий выбранный тип сортировки; по умолчанию "date_desc" (Новые)
    private var selectedSort: String = "date_desc"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNewsBinding.bind(view)

        setupRecyclerView()
        setupFilters()

        // При свайпе вниз — перезагрузить
        binding.swipeRefresh.setOnRefreshListener {
            loadNews()
        }

        // Сразу загружаем новости
        binding.swipeRefresh.isRefreshing = true
        loadNews()
    }

    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter()
        binding.rvNews.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = newsAdapter
        }
    }

    private fun setupFilters() {
        binding.chipNew.setOnClickListener {
            selectedSort = "date_desc"
            loadNews()
        }
        binding.chipOld.setOnClickListener {
            selectedSort = "date_asc"
            loadNews()
        }
        binding.chipPopular.setOnClickListener {
            selectedSort = "popular"
            loadNews()
        }
        binding.chipRecommended.setOnClickListener {
            selectedSort = "recommended"
            loadNews()
        }
    }

    private fun loadNews() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = repository.getNews(selectedSort)
                if (response.isSuccessful) {
                    val newsList = response.body() ?: emptyList()
                    newsAdapter.submitList(newsList)
                } else {
                    Toast.makeText(requireContext(), "Ошибка при загрузке новостей", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
