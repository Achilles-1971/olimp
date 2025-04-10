package com.example.olimp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.olimp.R
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.FragmentProfileBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.profile.EditProfileFragment
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

        binding.btnSettings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.btnEditProfile.setOnClickListener {
            Log.d("ProfileFragment", "Переход на EditProfileFragment")
            val editProfileFragment = EditProfileFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        // Перезагружаем профиль при возвращении на фрагмент
        loadUserProfile()
    }

    private fun loadUserProfile() {
        binding.progressBarProfile.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userResponse = userRepository.getCurrentUser()
                if (userResponse.isSuccessful) {
                    val user = userResponse.body()
                    Log.d("ProfileFragment", "Получен пользователь: $user")
                    Log.d("ProfileFragment", "Avatar получен: ${user?.avatar}")
                    if (user != null) {
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