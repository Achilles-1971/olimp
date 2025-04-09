package com.example.olimp.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.FragmentFriendsListBinding
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class FriendsListFragment : Fragment() {

    private var _binding: FragmentFriendsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FriendsAdapter
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsListBinding.inflate(inflater, container, false)
        userRepository = UserRepository(RetrofitInstance.getApi(requireContext()))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FriendsAdapter(
            onAction1Click = { friendship -> removeFriend(friendship.id) },
            onAction2Click = { friendship ->
                // TODO: Запуск чата/написать сообщение
            },
            isRequests = false
        )
        binding.rvFriends.layoutManager = LinearLayoutManager(context)
        binding.rvFriends.adapter = adapter

        loadFriends()
    }

    private fun loadFriends() {
        lifecycleScope.launch {
            try {
                val response = userRepository.getFriendList()
                if (response.isSuccessful) {
                    val friends = response.body() ?: emptyList()
                    adapter.submitList(friends)
                    binding.tvEmpty.visibility = if (friends.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    private fun removeFriend(friendshipId: Int) {
        lifecycleScope.launch {
            try {
                userRepository.removeFriend(friendshipId)
                loadFriends()
            } catch (e: Exception) {
                // Обработка ошибки
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
