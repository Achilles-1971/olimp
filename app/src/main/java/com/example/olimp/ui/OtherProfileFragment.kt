package com.example.olimp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.olimp.R
import com.example.olimp.data.models.UserResponse
import com.example.olimp.databinding.FragmentOtherProfileBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class OtherProfileFragment : Fragment() {

    private lateinit var binding: FragmentOtherProfileBinding
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем userId другого пользователя из аргументов
        userId = arguments?.getInt("userId") ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_other_profile, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserProfile()

        // Обработчики кнопок для других пользователей
        binding.btnAddFriend.setOnClickListener {
            // Реализуйте отправку заявки в друзья для userId
            Toast.makeText(context, "Запрос в друзья отправлен", Toast.LENGTH_SHORT).show()
        }

        binding.btnMessage.setOnClickListener {
            // Реализуйте переход на экран сообщений для userId
            Toast.makeText(context, "Переход к сообщениям", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile() {
        coroutineScope.launch {
            try {
                // Проверяем наличие токена (заголовок будет добавлен автоматически)
                val token = SessionManager(requireContext()).getAuthToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(context, "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val response = RetrofitInstance.getApi(requireContext()).getUserById(userId)
                if (response.isSuccessful) {
                    val user: UserResponse? = response.body()
                    if (user != null) {
                        binding.user = user
                    } else {
                        Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show()
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
