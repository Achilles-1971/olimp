package com.example.olimp.ui.events

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.olimp.data.models.CreateEventRequest
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.databinding.ActivityCreateEventBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.TimeZone

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var viewModel: EventsViewModel
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: SelectedImagesAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var startDateTime: Calendar? = null
    private var endDateTime: Calendar? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var marker: Marker? = null
    private lateinit var gestureDetector: GestureDetectorCompat

    private val IMAGE_PICK_REQUEST_CODE = 1001
    private val UCROP_REQUEST_CODE = UCrop.REQUEST_CROP
    private val LOCATION_PERMISSION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        val repository = EventsRepository(RetrofitInstance.getApi(this))
        val factory = EventsViewModel.EventsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[EventsViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Настройка RecyclerView и адаптера
        imagesAdapter = SelectedImagesAdapter()
        binding.rvSelectedImages.apply {
            adapter = imagesAdapter
            layoutManager = LinearLayoutManager(this@CreateEventActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // Обработчик удаления изображения
        imagesAdapter.setOnDeleteClickListener { position ->
            val removedUri = selectedImageUris[position]
            selectedImageUris.removeAt(position)
            imagesAdapter.setData(selectedImageUris)

            val previewUri = imagesAdapter.getSelectedUri()
            if (previewUri != null) {
                Glide.with(this).load(previewUri).into(binding.ivEventImage)
            } else if (selectedImageUris.isNotEmpty()) {
                imagesAdapter.selectItem(0)
                Glide.with(this).load(selectedImageUris[0]).into(binding.ivEventImage)
            } else {
                binding.ivEventImage.setImageDrawable(null)
            }
        }

        // Drag & Drop
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                Collections.swap(selectedImageUris, from, to)
                imagesAdapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.rvSelectedImages)

        // Клик выбора превью
        imagesAdapter.setOnItemClickListener { uri, _ ->
            Glide.with(this).load(uri).into(binding.ivEventImage)
        }

        // Инициализация карты
        initMap()

        // Переключение видимости карты и получение текущей геопозиции
        binding.btnSelectLocation.setOnClickListener {
            if (binding.mapView.visibility == View.VISIBLE) {
                binding.mapView.visibility = View.GONE
            } else {
                binding.mapView.visibility = View.VISIBLE
                requestCurrentLocation()
            }
        }

        // Отключение прокрутки ScrollView, когда карта активна
        binding.mapView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            (binding.root as ScrollView).requestDisallowInterceptTouchEvent(true) // Отключаем прокрутку ScrollView
            false
        }

        // Выбор фото
        binding.btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
        }

        // Обработка ввода даты
        binding.etStartDate.addTextChangedListener(DateTimeTextWatcher(binding.etStartDate))
        binding.etEndDate.addTextChangedListener(DateTimeTextWatcher(binding.etEndDate))

        // Выбор даты через диалог
        binding.btnStartDatePicker.setOnClickListener {
            showDateTimePicker(true) { calendar ->
                startDateTime = calendar
                binding.etStartDate.setText(formatDateTime(calendar))
            }
        }
        binding.btnEndDatePicker.setOnClickListener {
            showDateTimePicker(false) { calendar ->
                endDateTime = calendar
                binding.etEndDate.setText(formatDateTime(calendar))
            }
        }

        // Создание мероприятия
        binding.btnCreateEvent.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val startDateText = binding.etStartDate.text.toString().trim()
            val endDateText = binding.etEndDate.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Введите название мероприятия", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (startDateText.isEmpty()) {
                Toast.makeText(this, "Введите дату и время начала", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startDateTime = parseDateTime(startDateText)
            if (startDateTime == null) {
                Toast.makeText(
                    this,
                    "Неверный формат даты начала. Используйте: дд.мм.гггг чч:мм",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (endDateText.isNotEmpty()) {
                endDateTime = parseDateTime(endDateText)
                if (endDateTime == null) {
                    Toast.makeText(
                        this,
                        "Неверный формат даты окончания. Используйте: дд.мм.гггг чч:мм",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                if (endDateTime!!.before(startDateTime)) {
                    Toast.makeText(this, "Дата окончания не может быть раньше даты начала", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                endDateTime = null
            }

            val userId = sessionManager.getUserId()
            if (userId == null) {
                Toast.makeText(this, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maxParticipantsText = binding.etMaxParticipants.text.toString().trim()
            val maxParticipants = if (maxParticipantsText.isNotEmpty()) {
                try {
                    maxParticipantsText.toInt()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Макс. участников должно быть числом", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                0
            }

            val request = CreateEventRequest(
                title = title,
                subheader = binding.etSubheader.text.toString().trim().takeIf { it.isNotEmpty() },
                description = binding.etDescription.text.toString().trim().takeIf { it.isNotEmpty() },
                startDatetime = formatDateTimeToIso(startDateTime!!),
                endDatetime = endDateTime?.let { formatDateTimeToIso(it) },
                organizer = userId,
                address = binding.etAddress.text.toString().trim().takeIf { it.isNotEmpty() },
                latitude = selectedLatitude,
                longitude = selectedLongitude,
                maxParticipants = maxParticipants
            )

            viewModel.createEvent(request) { event ->
                val previewUri = imagesAdapter.getSelectedUri()
                if (previewUri != null) {
                    val previewPart = createPhotoPart(previewUri)
                    if (previewPart != null) {
                        viewModel.updateEventPreview(event.id, previewPart) { updatedEvent ->
                            val galleryUris = selectedImageUris.filter { it != previewUri }
                            if (galleryUris.isNotEmpty()) {
                                val galleryParts = createPhotoParts(galleryUris)
                                viewModel.uploadEventPhotos(event.id, galleryParts) { uploadedPhotos ->
                                    Toast.makeText(
                                        this,
                                        "Мероприятие создано с превью и ${uploadedPhotos.size} фото",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            } else {
                                Toast.makeText(this, "Мероприятие создано с превью", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Ошибка подготовки превью-изображения", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (selectedImageUris.isNotEmpty()) {
                        val galleryParts = createPhotoParts(selectedImageUris)
                        viewModel.uploadEventPhotos(event.id, galleryParts) { uploadedPhotos ->
                            Toast.makeText(
                                this,
                                "Мероприятие создано с ${uploadedPhotos.size} фото",
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
        }

        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, "Ошибка: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestCurrentLocation() {
        fetchCurrentLocation()
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Разрешение предоставлено, получаем текущее местоположение
            fusedLocationClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                if (location != null) {
                    setLocationOnMap(location.latitude, location.longitude, "Ваше местоположение")
                } else {
                    Toast.makeText(this, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка получения местоположения: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Разрешение не предоставлено
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Показываем объяснение, почему нужно разрешение
                Toast.makeText(this, "Разрешение на местоположение необходимо для точного выбора локации", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else if (!sessionManager.hasRequestedLocationPermission()) {
                // Первый запрос разрешения
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
                sessionManager.setLocationPermissionRequested(true) // Сохраняем, что запрос был сделан
            } else {
                // Пользователь окончательно отказался, используем Курск как запасной вариант
                setLocationOnMap(51.7373, 36.1874, "Курск (по умолчанию)")
                Toast.makeText(this, "Доступ к местоположению отклонен, используется Курск по умолчанию", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Вспомогательный метод для установки местоположения на карте
    private fun setLocationOnMap(latitude: Double, longitude: Double, markerTitle: String) {
        val geoPoint = GeoPoint(latitude, longitude)
        binding.mapView.controller.setCenter(geoPoint)
        binding.mapView.controller.setZoom(15.0)

        marker?.let { binding.mapView.overlays.remove(it) }
        marker = Marker(binding.mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = markerTitle
        }
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()

        selectedLatitude = latitude
        selectedLongitude = longitude
        binding.tvLatitude.text = "Широта: $selectedLatitude"
        binding.tvLongitude.text = "Долгота: $selectedLongitude"
        getAddressFromCoordinates(selectedLatitude!!, selectedLongitude!!) { address ->
            binding.etAddress.setText(address)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation() // Разрешение получено
            } else {
                fetchCurrentLocation() // Разрешение отклонено, сработает логика с Курском
            }
        }
    }

    private inner class DateTimeTextWatcher(private val editText: EditText) : TextWatcher {
        private var isFormatting = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isFormatting) return
            isFormatting = true

            val input = s.toString().replace("[^0-9]".toRegex(), "")
            val formatted = StringBuilder()
            if (input.isNotEmpty()) {
                formatted.append(input.substring(0, minOf(2, input.length)))
                if (input.length >= 3) {
                    formatted.append(".")
                    formatted.append(input.substring(2, minOf(4, input.length)))
                }
                if (input.length >= 5) {
                    formatted.append(".")
                    formatted.append(input.substring(4, minOf(8, input.length)))
                }
                if (input.length >= 9) {
                    formatted.append(" ")
                    formatted.append(input.substring(8, minOf(10, input.length)))
                }
                if (input.length >= 11) {
                    formatted.append(":")
                    formatted.append(input.substring(10, minOf(12, input.length)))
                }
            }

            editText.removeTextChangedListener(this)
            editText.setText(formatted.toString())
            editText.setSelection(formatted.length)
            editText.addTextChangedListener(this)

            isFormatting = false
        }
    }

    private fun parseDateTime(dateTimeStr: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = sdf.parse(dateTimeStr)
            Calendar.getInstance().apply {
                time = date
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun initMap() {
        Configuration.getInstance().userAgentValue = packageName
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(GeoPoint(55.7558, 37.6173))

        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val touchPoint = binding.mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                marker?.let { binding.mapView.overlays.remove(it) }
                marker = Marker(binding.mapView).apply {
                    position = touchPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Выбранное местоположение"
                }
                binding.mapView.overlays.add(marker)
                binding.mapView.invalidate()
                selectedLatitude = touchPoint.latitude
                selectedLongitude = touchPoint.longitude
                binding.tvLatitude.text = "Широта: $selectedLatitude"
                binding.tvLongitude.text = "Долгота: $selectedLongitude"
                getAddressFromCoordinates(selectedLatitude!!, selectedLongitude!!) { address ->
                    binding.etAddress.setText(address)
                }
            }
        })
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double, onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", packageName)
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val address = json.optString("display_name", "Адрес не найден")
                    withContext(Dispatchers.Main) { onResult(address) }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateEventActivity, "Не удалось получить адрес", Toast.LENGTH_SHORT).show()
                        onResult("Адрес не найден")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateEventActivity, "Ошибка получения адреса: ${e.message}", Toast.LENGTH_SHORT).show()
                    onResult("Адрес не найден")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMAGE_PICK_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val clipData = data.clipData
                    if (clipData != null) {
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            launchImageCropper(imageUri)
                        }
                    } else {
                        data.data?.let { launchImageCropper(it) }
                    }
                }
            }
            UCROP_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let { croppedUri ->
                        selectedImageUris.add(croppedUri)
                        imagesAdapter.setData(selectedImageUris)
                        if (imagesAdapter.getSelectedUri() == null && selectedImageUris.size == 1) {
                            imagesAdapter.selectItem(0)
                            Glide.with(this).load(croppedUri).into(binding.ivEventImage)
                        }
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    val cropError = UCrop.getError(data!!)
                    Toast.makeText(this, "Ошибка обрезки: ${cropError?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchImageCropper(sourceUri: Uri) {
        val destinationFileName = "CroppedImage_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(800, 800)
            .start(this)
    }

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

    private fun createPhotoPart(uri: Uri): MultipartBody.Part? {
        return getFileFromUri(uri)?.let {
            val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", it.name, requestFile)
        }
    }

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

    private fun showDateTimePicker(isStart: Boolean, onDateTimeSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val selectedCalendar = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onDateTimeSelected(selectedCalendar)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDateTime(calendar: Calendar): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun formatDateTimeToIso(calendar: Calendar): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(calendar.time)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}