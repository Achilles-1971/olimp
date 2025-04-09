package com.example.olimp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.olimp.R
import com.example.olimp.data.models.Friendship
import com.example.olimp.data.models.UserResponse
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.FragmentOtherProfileBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.example.olimp.network.ApiService

class OtherProfileFragment : Fragment() {

    private lateinit var binding: FragmentOtherProfileBinding
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: Int = 0
    private lateinit var userRepository: UserRepository

    // Возможные статусы дружбы:
    // "none" – связи нет,
    // "pending_sent" – текущий пользователь отправил заявку,
    // "pending_received" – заявка получена от этого пользователя,
    // "accepted" – уже друзья.
    private var friendshipStatus: String = "none"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем userId другого пользователя из аргументов
        userId = arguments?.getInt("userId") ?: 0
        userRepository = UserRepository(RetrofitInstance.getApi(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_other_profile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfileAndFriendshipStatus()

        // Обработчики кнопок для других пользователей
        binding.btnAddFriend.setOnClickListener {
            when (friendshipStatus) {
                "none" -> sendFriendRequest()
                "pending_sent" -> cancelFriendRequest()
                "pending_received" -> acceptFriendRequest()
                "accepted" -> Toast.makeText(context, "Уже друзья", Toast.LENGTH_SHORT).show()
                else -> sendFriendRequest()
            }
        }

        binding.btnMessage.setOnClickListener {
            val context = requireContext()
            val intent = android.content.Intent(context, com.example.olimp.ui.messages.MessageActivity::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }
    }

    private fun loadUserProfileAndFriendshipStatus() {
        coroutineScope.launch {
            try {
                val token = SessionManager(requireContext()).getAuthToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(context, "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // Загрузка профиля пользователя
                val userResponse = userRepository.getUserById(userId)
                if (userResponse.isSuccessful) {
                    val user = userResponse.body()
                    if (user != null) {
                        binding.user = user
                    } else {
                        Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Ошибка загрузки профиля: ${userResponse.code()}", Toast.LENGTH_SHORT).show()
                }
                // Загрузка списка дружеских связей
                val friendshipsResponse = userRepository.getFriendships()
                if (friendshipsResponse.isSuccessful) {
                    val friendships = friendshipsResponse.body() ?: emptyList()
                    val currentUserId = SessionManager(requireContext()).getUserId() ?: 0
                    val friendship = friendships.find {
                        (it.user.id == currentUserId && it.friend.id == userId) ||
                                (it.friend.id == currentUserId && it.user.id == userId)
                    }
                    friendshipStatus = when {
                        friendship == null -> "none"
                        friendship.status == "pending" && friendship.user.id == currentUserId -> "pending_sent"
                        friendship.status == "pending" && friendship.friend.id == currentUserId -> "pending_received"
                        friendship.status == "accepted" -> "accepted"
                        else -> "none"
                    }
                    updateFriendshipButton()
                } else {
                    Toast.makeText(context, "Ошибка загрузки статуса дружбы: ${friendshipsResponse.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFriendshipButton() {
        when (friendshipStatus) {
            "none" -> binding.btnAddFriend.text = "Добавить в друзья"
            "pending_sent" -> binding.btnAddFriend.text = "Заявка отправлена"
            "pending_received" -> binding.btnAddFriend.text = "Принять заявку"
            "accepted" -> binding.btnAddFriend.text = "Друзья"
            else -> binding.btnAddFriend.text = "Добавить в друзья"
        }
    }

    private fun sendFriendRequest() {
        coroutineScope.launch {
            try {
                val response = userRepository.sendFriendRequest(userId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Заявка отправлена", Toast.LENGTH_SHORT).show()
                    loadUserProfileAndFriendshipStatus()
                } else {
                    Toast.makeText(context, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelFriendRequest() {
        coroutineScope.launch {
            try {
                val response = userRepository.cancelFriendRequest(userId)
                if (response.isSuccessful) {
                    Toast.makeText(context, "Заявка отменена", Toast.LENGTH_SHORT).show()
                    loadUserProfileAndFriendshipStatus()
                } else {
                    Toast.makeText(context, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun acceptFriendRequest() {
        coroutineScope.launch {
            try {
                // Получаем список дружеских связей, чтобы найти нужное
                val friendshipsResponse = userRepository.getFriendships()
                if (friendshipsResponse.isSuccessful) {
                    val friendships = friendshipsResponse.body() ?: emptyList()
                    val currentUserId = SessionManager(requireContext()).getUserId() ?: 0
                    val friendship = friendships.find {
                        (it.user.id == currentUserId && it.friend.id == userId) ||
                                (it.friend.id == currentUserId && it.user.id == userId)
                    }
                    val friendshipId = friendship?.id ?: return@launch
                    val response = userRepository.acceptFriendRequest(friendshipId)
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Заявка принята", Toast.LENGTH_SHORT).show()
                        loadUserProfileAndFriendshipStatus()
                    } else {
                        Toast.makeText(context, "Ошибка: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Ошибка загрузки статуса дружбы", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
    }
}
