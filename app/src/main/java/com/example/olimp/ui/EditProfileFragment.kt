package com.example.olimp.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.olimp.data.repository.UserRepository
import com.example.olimp.databinding.FragmentEditProfileBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.RealPathUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val userRepository by lazy { UserRepository(RetrofitInstance.getApi(requireContext())) }

    private var avatarUri: Uri? = null
    private var userId: Int = 0

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == -1) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                avatarUri = it
                binding.ivEditAvatar.setImageURI(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentUser()

        binding.ivEditAvatar.setOnClickListener { pickImage() }
        binding.ivEditIcon.setOnClickListener { pickImage() }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadCurrentUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            val response = userRepository.getCurrentUser()
            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    userId = user.id
                    binding.etUsername.setText(user.username)
                    binding.etBio.setText(user.bio ?: "")
                    user.avatar?.let {
                        com.bumptech.glide.Glide.with(this@EditProfileFragment)
                            .load(it)
                            .into(binding.ivEditAvatar)
                    }
                }
            } else {
                showToast("Не удалось загрузить профиль")
            }
        }
    }

    private fun pickImage() {
        imagePicker.launch("image/*")
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_avatar.jpg"))
        val options = UCrop.Options().apply {
            setCompressionQuality(90)
            setCircleDimmedLayer(true)
            setToolbarTitle("Обрезка фото")
        }

        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(600, 600)
            .withOptions(options)

        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fields = mapOf(
                    "username" to username,
                    "bio" to bio
                )
                Log.d("EditProfileFragment", "Собранные данные: $fields")

                var avatarPart: MultipartBody.Part? = null
                avatarUri?.let { uri ->
                    // Используем ContentResolver для получения InputStream
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        Log.e("EditProfileFragment", "Не удалось открыть InputStream для аватарки")
                        showToast("Ошибка при загрузке аватарки")
                        return@launch
                    }

                    // Преобразуем InputStream в ByteArray
                    val byteArray = inputStream.readBytes()
                    inputStream.close()

                    // Создаём RequestBody из байтов
                    val requestBody = byteArray.toRequestBody("image/*".toMediaTypeOrNull())
                    // Используем имя файла по умолчанию или извлекаем из Uri
                    val fileName = requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "avatar.jpg"

                    avatarPart = MultipartBody.Part.createFormData("avatar", fileName, requestBody)
                    Log.d("EditProfileFragment", "Аватарка подготовлена: $fileName")
                }

                Log.d("EditProfileFragment", "Отправка запроса: fields=$fields, avatar=${avatarPart != null}")
                val response = userRepository.updateUserProfile(userId, fields, avatarPart)
                Log.d("EditProfileFragment", "Получен ответ: код=${response.code()}, тело=${response.body()}")

                if (response.isSuccessful) {
                    showToast("Профиль обновлён")
                    Log.d("EditProfileFragment", "Профиль успешно обновлен: ${response.body()}")
                    parentFragmentManager.popBackStack()
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Ошибка при обновлении профиля: ${response.code()}")
                    Log.e("EditProfileFragment", "Ошибка: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Ошибка: ${e.message}")
                Log.e("EditProfileFragment", "Исключение: ${e.message}", e)
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}