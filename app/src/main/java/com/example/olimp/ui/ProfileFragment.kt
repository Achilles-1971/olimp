package com.example.olimp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.FragmentProfileBinding
import com.example.olimp.network.RetrofitInstance
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userRepository = UserRepository(RetrofitInstance.getApi(requireContext()))
        loadUserProfile()
    }

    private fun loadUserProfile() {
        // Показываем прогресс-бар
        binding.progressBarProfile.visibility = View.VISIBLE

        // Запускаем корутину во viewLifecycleOwner.lifecycleScope
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userResponse = userRepository.getCurrentUser()
                if (userResponse.isSuccessful) {
                    val user = userResponse.body()
                    Log.d("ProfileFragment", "Получен пользователь: $user")
                    // Добавляем логирование поля avatar
                    Log.d("ProfileFragment", "Avatar получен: ${user?.avatar}")
                    if (user != null) {
                        // Обновляем DataBinding только если _binding ещё существует
                        _binding?.user = user
                    } else {
                        context?.let {
                            android.widget.Toast.makeText(it, "Пользователь не найден", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    context?.let {
                        android.widget.Toast.makeText(it, "Ошибка загрузки профиля: ${userResponse.code()}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                context?.let {
                    android.widget.Toast.makeText(it, "Ошибка: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                Log.e("ProfileFragment", "Ошибка при загрузке профиля", e)
            } finally {
                _binding?.progressBarProfile?.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
