package com.example.olimp.ui.events

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.olimp.data.models.CreateEventRequest
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.databinding.ActivityCreateEventBinding
import com.example.olimp.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Calendar

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var viewModel: EventsViewModel
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: SelectedImagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация ViewModel
        val repository = EventsRepository(RetrofitInstance.getApi(this))
        val factory = EventsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[EventsViewModel::class.java]

        // Адаптер для миниатюр
        imagesAdapter = SelectedImagesAdapter()
        binding.rvSelectedImages.adapter = imagesAdapter
        binding.rvSelectedImages.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Кнопка «Добавить фото»
        binding.btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, 1001)
        }

        // Поля для дат
        binding.etStartDate.setOnClickListener {
            showDatePicker { y, m, d ->
                binding.etStartDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }
        }
        binding.etEndDate.setOnClickListener {
            showDatePicker { y, m, d ->
                binding.etEndDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }
        }

        // Кнопка «Создать мероприятие»
        binding.btnCreateEvent.setOnClickListener {
            val request = CreateEventRequest(
                title = binding.etTitle.text.toString(),
                description = binding.etDescription.text.toString(),
                startDatetime = binding.etStartDate.text.toString(),
                endDatetime = binding.etEndDate.text.toString(),
                address = binding.etAddress.text.toString(),
                latitude = null,   // при желании сюда можно передать координаты
                longitude = null
            )

            viewModel.createEvent(request) { event ->
                // Если мероприятие создано
                if (selectedImageUris.isNotEmpty()) {
                    // Загружаем фотографии
                    val parts = createPhotoParts(selectedImageUris)
                    viewModel.uploadEventPhotos(event.id, parts) { uploadedPhotos ->
                        Toast.makeText(
                            this,
                            "Мероприятие и фото загружены: ${uploadedPhotos.size}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Мероприятие создано (без фото)", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        // Подписка на ошибки
        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, "Ошибка: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Выбор картинок
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            data.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedImageUris.add(uri)
                }
            } ?: data.data?.let { uri ->
                selectedImageUris.add(uri)
            }

            imagesAdapter.setData(selectedImageUris)

            if (selectedImageUris.isNotEmpty()) {
                Glide.with(this)
                    .load(selectedImageUris.first())
                    .into(binding.ivEventImage)
            }
        }
    }

    // Преобразуем Uri -> MultipartBody.Part
    private fun createPhotoParts(uris: List<Uri>): List<MultipartBody.Part> {
        val parts = mutableListOf<MultipartBody.Part>()

        for (uri in uris) {
            val file = getFileFromUri(uri)
            file?.let {
                val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photos", it.name, requestFile)
                parts.add(part)
            }
        }
        return parts
    }

    // Копируем файл из Uri во временный File
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File.createTempFile("upload", ".jpg", cacheDir)
            file.outputStream().use { out ->
                inputStream?.copyTo(out)
            }
            inputStream?.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // DatePicker
    private fun showDatePicker(onDateSelected: (year: Int, month: Int, day: Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val dp = DatePickerDialog(
            this,
            { _, year, month, day ->
                onDateSelected(year, month, day)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dp.show()
    }
}
