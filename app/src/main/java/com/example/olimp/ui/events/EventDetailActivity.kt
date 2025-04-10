package com.example.olimp.ui.events

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.data.models.Comment
import com.example.olimp.data.models.CommentRequest
import com.example.olimp.data.models.Event
import com.example.olimp.data.models.FlatComment
import com.example.olimp.data.repository.CommentRepository
import com.example.olimp.data.repository.EventsRepository
import com.example.olimp.databinding.ActivityEventsDetailBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.ui.CommentsBottomSheetDialogFragment
import com.example.olimp.ui.FlatCommentAdapter
import com.example.olimp.ui.LoginActivity
import com.example.olimp.ui.OtherProfileFragment
import com.example.olimp.ui.ProfileFragment
import com.example.olimp.ui.ProfileHostActivity
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class EventDetailActivity : AppCompatActivity(), CommentsBottomSheetDialogFragment.CommentUpdateListener {

    private lateinit var binding: ActivityEventsDetailBinding
    private lateinit var eventsRepository: EventsRepository
    private lateinit var commentRepository: CommentRepository
    private lateinit var flatCommentAdapter: FlatCommentAdapter
    private lateinit var photoPagerAdapter: EventPhotoPagerAdapter

    private var eventId: Int = 0
    private var event: Event? = null
    private var totalComments: Int = 0
    private var replyingToCommentId: Int? = null
    private var isRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        photoPagerAdapter = EventPhotoPagerAdapter(emptyList())
        binding.viewPagerPhotos.adapter = photoPagerAdapter

        eventId = intent.getIntExtra("EVENT_ID", 0)
        if (eventId == 0) {
            Log.e("EventDetailActivity", "Invalid eventId received: $eventId. Check the push notification data.")
            Toast.makeText(this, getString(R.string.error_invalid_event_id), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        eventsRepository = EventsRepository(RetrofitInstance.getApi(this))
        commentRepository = CommentRepository(RetrofitInstance.getApi(this))

        // (1) Инициализация списка комментариев
        binding.rvCommentsPreview.layoutManager = LinearLayoutManager(this)
        flatCommentAdapter = FlatCommentAdapter(mutableListOf()) { flatComment, action ->
            when (action) {
                "profile" -> openUserProfile(flatComment.comment.user?.id)
                "like"    -> toggleCommentLike(flatComment)
                "delete"  -> deleteComment(flatComment)
                "reply"   -> showCommentsBottomSheetWithReply(flatComment.comment.id)
                "edit"    -> openEditDialog(flatComment)
            }
        }
        binding.rvCommentsPreview.adapter = flatCommentAdapter

        // (2) Тап по блоку «Написать комментарий...», открываем BottomSheet
        //    (аналогично, если вы оставляете старый llCommentInput — тогда используйте именно его)
        binding.llCommentInputPreview.setOnClickListener {
            showCommentsBottomSheet()
        }

        binding.btnParticipate.setOnClickListener {
            if (isEventFinished()) {
                Toast.makeText(this, "Нельзя записаться на завершённое мероприятие", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isRegistered) cancelParticipation() else participateInEvent()
        }


        // (4) Заголовок «Комментарии»
        binding.tvCommentsHeader.setOnClickListener {
            showCommentsBottomSheet()
        }

        // (5) Открыть адрес в картах
        binding.btnOpenMap.setOnClickListener {
            val address = binding.tvLocation.text.toString()
            if (address == getString(R.string.address_not_specified)) {
                Toast.makeText(this, getString(R.string.no_address_available), Toast.LENGTH_SHORT).show()
            } else {
                val uri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, getString(R.string.no_map_app), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // (6) Скопировать адрес
        binding.btnCopyAddress.setOnClickListener {
            val address = binding.tvLocation.text.toString()
            if (address != getString(R.string.address_not_specified)) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("address", address)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.address_copied), Toast.LENGTH_SHORT).show()
            }
        }

        // (7) Сворачивание / разворачивание плашки «Подробности мероприятия»
        //     Сначала скрываем контент (либо можно сделать «по умолчанию развернуто»)
        var isDetailsExpanded = false
        binding.detailsContent.visibility = View.GONE
        binding.ivExpandIcon.setImageResource(android.R.drawable.arrow_down_float)

        binding.detailsHeader.setOnClickListener {
            isDetailsExpanded = !isDetailsExpanded
            binding.detailsContent.visibility = if (isDetailsExpanded) View.VISIBLE else View.GONE
            binding.ivExpandIcon.setImageResource(
                if (isDetailsExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )
        }

        // Загружаем детали
        loadEventDetails()
        // Загружаем последний комментарий (превью)
        loadLatestComment()
        // Отмечаем просмотр (view) при необходимости
        registerViewIfNeeded()

        // Анимация кнопки «Участвовать»
        binding.btnParticipate.postDelayed({
            updateParticipateButton()
        }, 300)
    }



    private fun updateParticipateButton() {
        val button = binding.btnParticipate

        if (isEventFinished()) {
            button.isEnabled = false
            button.text = "Мероприятие завершено"
            button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.gray)
            return
        }

        val newText = if (isRegistered) getString(R.string.cancel_participation_button) else getString(R.string.participate_button)
        val newColor = ContextCompat.getColorStateList(this, if (isRegistered) R.color.gray else R.color.gold)

        button.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                button.text = newText
                button.backgroundTintList = newColor
                button.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }


    private fun temporarilyDisableParticipateButton(durationMillis: Long = 1500L) {
        binding.btnParticipate.isEnabled = false
        binding.btnParticipate.postDelayed({
            binding.btnParticipate.isEnabled = true
        }, durationMillis)
    }

    private fun participateInEvent() {
        temporarilyDisableParticipateButton() // ← 🔒 блокируем на старте
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    eventsRepository.registerForEvent(eventId)
                }
                if (response.code() == 201 || response.code() == 200) {
                    isRegistered = true
                    updateParticipateButton()
                    Toast.makeText(this@EventDetailActivity, R.string.registered_successfully, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EventDetailActivity, getString(R.string.error_code, response.code()), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelParticipation() {
        temporarilyDisableParticipateButton()
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    eventsRepository.cancelParticipation(eventId)
                }
                if (response.isSuccessful) {
                    isRegistered = false
                    updateParticipateButton()
                    Toast.makeText(this@EventDetailActivity, R.string.cancelled_successfully, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EventDetailActivity, getString(R.string.error_code, response.code()), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadEventDetails() {
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                event = withContext(Dispatchers.IO) { eventsRepository.getEventById(eventId) }
                event?.let { evt ->
                    isRegistered = evt.isRegistered == true
                    binding.tvTitle.text = evt.title

                    val rawDescription = evt.description ?: getString(R.string.no_description)
                    val escaped = android.text.Html.escapeHtml(rawDescription)
                    val displayed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        android.text.Html.fromHtml(escaped, android.text.Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        @Suppress("DEPRECATION")
                        android.text.Html.fromHtml(escaped)
                    }
                    binding.tvFullContent.text = displayed

                    binding.tvViews.text = getString(R.string.views_count, evt.viewsCount ?: 0)
                    photoPagerAdapter.setData(evt.photos ?: emptyList())

                    binding.tvSubheader.visibility = if (evt.subheader.isNullOrEmpty()) View.GONE else View.VISIBLE
                    binding.tvSubheader.text = evt.subheader

                    val address = if (!evt.address.isNullOrBlank()) evt.address else getString(R.string.address_not_specified)
                    binding.tvLocation.text = address

                    binding.tvMaxParticipants.text = "Макс. участников: ${evt.maxParticipants ?: 0}"

                    // Форматирование дат через OffsetDateTime
                    val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    val outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

                    // Начало
                    try {
                        evt.startDatetime?.let { start ->
                            val parsed = OffsetDateTime.parse(start, inputFormatter)
                            binding.tvStartDate.text = "Начало: ${parsed.format(outputFormatter)}"
                        } ?: run {
                            binding.tvStartDate.text = "Начало: Не указано"
                        }
                    } catch (e: Exception) {
                        Log.e("EventDetail", "Ошибка парсинга startDatetime: ${e.message}")
                        binding.tvStartDate.text = "Начало: Ошибка формата"
                    }

                    // Окончание
                    try {
                        evt.endDatetime?.let { end ->
                            val parsed = OffsetDateTime.parse(end, inputFormatter)
                            binding.tvEndDate.text = "Окончание: ${parsed.format(outputFormatter)}"
                        } ?: run {
                            binding.tvEndDate.text = "Окончание: Не указано"
                        }
                    } catch (e: Exception) {
                        Log.e("EventDetail", "Ошибка парсинга endDatetime: ${e.message}")
                        binding.tvEndDate.text = "Окончание: Ошибка формата"
                    }

                    // Организатор
                    evt.organizer?.let { org ->
                        binding.tvOrganizerName.text = org.username ?: "Неизвестный организатор"
                        org.avatar?.let { avatarUrl ->
                            Glide.with(this@EventDetailActivity)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_profile)
                                .into(binding.ivOrganizerAvatar)
                        } ?: run {
                            binding.ivOrganizerAvatar.setImageResource(R.drawable.ic_profile)
                        }
                        // Обновляем кликабельность аватарки и имени организатора
                        binding.ivOrganizerAvatar.setOnClickListener {
                            openUserProfile(org.id)
                        }
                        binding.tvOrganizerName.setOnClickListener {
                            openUserProfile(org.id)
                        }
                        binding.organizerLayout.setOnClickListener {
                            openUserProfile(org.id)
                        }
                    } ?: run {
                        binding.tvOrganizerName.text = "Организатор не указан"
                        binding.organizerLayout.isClickable = false
                    }
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Ошибка загрузки деталей мероприятия: ${e.message}")
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun isEventFinished(): Boolean {
        val end = event?.endDatetime ?: return false
        return try {
            val endTime = OffsetDateTime.parse(end, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            endTime.isBefore(OffsetDateTime.now())
        } catch (e: Exception) {
            Log.e("EventDetail", "Ошибка парсинга endDatetime в isEventFinished: ${e.message}")
            false
        }
    }


    private fun registerViewIfNeeded() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        val viewPrefs = getSharedPreferences("views", Context.MODE_PRIVATE)
        val viewKey = "viewed_event_$eventId"

        if (viewPrefs.getBoolean(viewKey, false)) {
            Log.d("EventDetail", "View already registered for eventId=$eventId")
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { eventsRepository.addView(eventId) }
                if (response.isSuccessful && response.body() != null) {
                    val viewsCount = response.body()!!.viewsCount
                    binding.tvViews.text = getString(R.string.views_count, viewsCount)
                    viewPrefs.edit().putBoolean(viewKey, true).apply()
                    Log.d("EventDetail", "View registered for eventId=$eventId, userId=$userId")
                } else {
                    Log.d("EventDetail", "Failed to register view, code=${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Error registering view: ${e.message}")
            }
        }
    }

    private fun loadLatestComment() {
        Log.d("EventDetailActivity", "loadLatestComment called")
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { commentRepository.getComments("event", eventId, 1) }
                if (response.isSuccessful && response.body() != null) {
                    val paginatedResponse = response.body()!!
                    totalComments = paginatedResponse.count
                    binding.tvCommentsHeader.text = getString(R.string.comments_header, totalComments)
                    if (paginatedResponse.results.isNotEmpty()) {
                        val latestComment = paginatedResponse.results.first()
                        val newList = listOf(FlatComment(latestComment, 0))
                        Log.d("EventDetailActivity", "loadLatestComment updating with: $newList")
                        flatCommentAdapter.updateData(newList)
                        binding.tvEmptyComments.visibility = View.GONE
                        binding.rvCommentsPreview.visibility = View.VISIBLE
                    } else {
                        Log.d("EventDetailActivity", "loadLatestComment: no comments")
                        flatCommentAdapter.updateData(emptyList())
                        binding.tvEmptyComments.visibility = View.VISIBLE
                        binding.rvCommentsPreview.visibility = View.GONE
                    }
                } else {
                    Log.d("EventDetailActivity", "loadLatestComment: failed response")
                    flatCommentAdapter.updateData(emptyList())
                    binding.tvCommentsHeader.text = getString(R.string.write_comment)
                    binding.tvEmptyComments.visibility = View.VISIBLE
                    binding.rvCommentsPreview.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Error loading latest comment: ${e.message}")
                flatCommentAdapter.updateData(emptyList())
                binding.tvEmptyComments.visibility = View.VISIBLE
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun showCommentsBottomSheet() {
        val fragment = CommentsBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putInt("entityId", eventId)
                putString("entityType", "event")
                replyingToCommentId?.let { putInt("parentCommentId", it) }
            }
        }
        fragment.show(supportFragmentManager, "CommentsBottomSheet")
        replyingToCommentId = null // Сбрасываем после открытия Bottom Sheet
    }

    private fun showCommentsBottomSheetWithReply(parentCommentId: Int) {
        replyingToCommentId = parentCommentId
        showCommentsBottomSheet()
    }

    private fun toggleCommentLike(flatComment: FlatComment) {
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { commentRepository.toggleCommentLike(flatComment.comment.id) }
                if (response.isSuccessful && response.body() != null) {
                    val toggleResponse = response.body()!!
                    val updatedComment = flatComment.comment.copy(
                        likesCount = toggleResponse.likes_count,
                        isLiked = toggleResponse.liked
                    )
                    val currentList = flatCommentAdapter.getCurrentList()
                    if (currentList.isNotEmpty() && currentList[0].comment.id == updatedComment.id) {
                        flatCommentAdapter.updateData(listOf(FlatComment(updatedComment, 0)))
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun deleteComment(flatComment: FlatComment) {
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { commentRepository.deleteComment(flatComment.comment.id) }
                if (response.isSuccessful) {
                    totalComments--
                    binding.tvCommentsHeader.text = if (totalComments > 0) {
                        getString(R.string.comments_header, totalComments)
                    } else {
                        getString(R.string.write_comment)
                    }
                    binding.tvEmptyComments.visibility = if (totalComments == 0) View.VISIBLE else View.GONE
                    binding.rvCommentsPreview.visibility = if (totalComments > 0) View.VISIBLE else View.GONE
                    if (totalComments > 0) {
                        loadLatestComment()
                    } else {
                        flatCommentAdapter.updateData(emptyList())
                    }
                    Toast.makeText(this@EventDetailActivity, getString(R.string.comment_deleted), Toast.LENGTH_SHORT).show()
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun openEditDialog(flatComment: FlatComment) {
        val editText = EditText(this).apply {
            setText(flatComment.comment.content)
            setSelection(flatComment.comment.content.length)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_comment))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateComment(flatComment, newText)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateComment(flatComment: FlatComment, newText: String) {
        val request = CommentRequest(
            entityId = eventId,
            content = newText,
            entityType = "event",
            parentCommentId = flatComment.comment.parentCommentId
        )
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) {
                    commentRepository.updateComment(flatComment.comment.id, request)
                }
                if (response.isSuccessful && response.body() != null) {
                    val updatedComment = response.body()!!
                    val currentList = flatCommentAdapter.getCurrentList()
                    if (currentList.isNotEmpty() && currentList[0].comment.id == updatedComment.id) {
                        flatCommentAdapter.updateData(listOf(FlatComment(updatedComment, 0)))
                    }
                    Toast.makeText(this@EventDetailActivity, getString(R.string.comment_updated), Toast.LENGTH_SHORT).show()
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun openUserProfile(userId: Int?) {
        userId ?: run {
            Toast.makeText(this, getString(R.string.no_user_data), Toast.LENGTH_SHORT).show()
            return
        }
        val sessionManager = SessionManager(this)
        val currentUserId = sessionManager.getUserId()
        Log.d("EventDetailActivity", "openUserProfile called with userId=$userId, currentUserId=$currentUserId")

        val intent = Intent(this, ProfileHostActivity::class.java)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun handleApiError(code: Int) {
        Toast.makeText(this, getString(R.string.error_code, code), Toast.LENGTH_SHORT).show()
        if (code == 401) {
            SessionManager(this).clearAuthToken()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCommentAdded(newComment: Comment) {
        Log.d("EventDetailActivity", "onCommentAdded called with comment: $newComment")
        totalComments++
        binding.tvCommentsHeader.text = getString(R.string.comments_header, totalComments)
        binding.tvEmptyComments.visibility = View.GONE
        binding.rvCommentsPreview.visibility = View.VISIBLE

        val currentComments = flatCommentAdapter.getCurrentList().toMutableList()
        Log.d("EventDetailActivity", "Before adding: size=${currentComments.size}, comments=$currentComments")
        currentComments.add(0, FlatComment(newComment, 0))
        Log.d("EventDetailActivity", "After adding: size=${currentComments.size}, comments=$currentComments")

        // Убираем updateData с DiffUtil, заменяем на прямое обновление
        flatCommentAdapter.getCurrentList().clear()
        flatCommentAdapter.getCurrentList().addAll(currentComments)
        flatCommentAdapter.notifyItemInserted(0)
        binding.rvCommentsPreview.layoutManager?.scrollToPosition(0)
        binding.rvCommentsPreview.requestLayout()
        Log.d("EventDetailActivity", "UI updated, adapter size=${flatCommentAdapter.itemCount}")
    }

    override fun onCommentUpdated(updatedComment: Comment, position: Int) {
        val currentList = flatCommentAdapter.getCurrentList()
        if (currentList.isNotEmpty() && currentList[0].comment.id == updatedComment.id) {
            flatCommentAdapter.updateData(listOf(FlatComment(updatedComment, 0)))
        }
    }

    override fun onCommentDeleted(position: Int) {
        totalComments--
        binding.tvCommentsHeader.text = if (totalComments > 0) {
            getString(R.string.comments_header, totalComments)
        } else {
            getString(R.string.write_comment)
        }
        binding.tvEmptyComments.visibility = if (totalComments == 0) View.VISIBLE else View.GONE
        binding.rvCommentsPreview.visibility = if (totalComments > 0) View.VISIBLE else View.GONE
        if (totalComments > 0) {
            loadLatestComment()
        } else {
            flatCommentAdapter.updateData(emptyList())
        }
    }

    override fun onCommentLiked(updatedComment: Comment, position: Int) {
        val currentList = flatCommentAdapter.getCurrentList()
        if (currentList.isNotEmpty() && currentList[0].comment.id == updatedComment.id) {
            flatCommentAdapter.updateData(listOf(FlatComment(updatedComment, 0)))
        }
    }
}