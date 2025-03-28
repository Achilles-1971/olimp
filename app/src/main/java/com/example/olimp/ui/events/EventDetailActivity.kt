package com.example.olimp.ui.events

import android.content.Context
import android.content.Intent
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
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        photoPagerAdapter = EventPhotoPagerAdapter(emptyList())
        binding.viewPagerPhotos.adapter = photoPagerAdapter


        eventId = intent.getIntExtra("EVENT_ID", 0)
        if (eventId == 0) {
            Toast.makeText(this, getString(R.string.error_invalid_event_id), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        eventsRepository = EventsRepository(RetrofitInstance.getApi(this))
        commentRepository = CommentRepository(RetrofitInstance.getApi(this))

        // Настройка адаптера для превью (только один комментарий)
        binding.rvCommentsPreview.layoutManager = LinearLayoutManager(this)
        flatCommentAdapter = FlatCommentAdapter(mutableListOf()) { flatComment, action ->
            when (action) {
                "profile" -> openUserProfile(flatComment.comment.user?.id)
                "like" -> toggleCommentLike(flatComment)
                "delete" -> deleteComment(flatComment)
                "reply" -> showCommentsBottomSheetWithReply(flatComment.comment.id)
                "edit" -> openEditDialog(flatComment)
                else -> {} // "expand" и "collapse" не используются в превью
            }
        }
        binding.rvCommentsPreview.adapter = flatCommentAdapter
        binding.rvCommentsPreview.setOnClickListener(null) // Отключаем случайные клики

        // Настройка поля ввода как триггера для Bottom Sheet
        binding.llCommentInput.background = ContextCompat.getDrawable(this, R.drawable.comment_input_background)
        binding.llCommentInput.clipToOutline = true
        binding.etCommentInput.isEnabled = true // Делаем EditText кликабельным
        binding.etCommentInput.keyListener = null // Отключаем ввод текста напрямую
        binding.etCommentInput.setOnClickListener { showCommentsBottomSheet() }
        binding.btnSendComment.visibility = View.GONE // Убираем кнопку отправки, так как она теперь в Bottom Sheet

        // Загрузка данных и настройка интерфейса
        loadEventDetails()
        loadLatestComment()
        registerViewIfNeeded()

        binding.btnParticipate.setOnClickListener {
            Toast.makeText(this, getString(R.string.participate_button_clicked, eventId), Toast.LENGTH_SHORT).show()
        }

        binding.tvCommentsHeader.setOnClickListener { showCommentsBottomSheet() }
    }

    private fun loadEventDetails() {
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                event = withContext(Dispatchers.IO) {
                    eventsRepository.getEventById(eventId)
                }

                if (event != null) {
                    binding.tvTitle.text = event!!.title
                    binding.tvFullContent.text = event!!.description ?: getString(R.string.no_description)
                    binding.tvViews.text = getString(R.string.views_count, event!!.viewsCount ?: 0)

                    // ⬇️ Подключаем фото
                    event!!.photos?.let { photoPagerAdapter.setData(it) }

                    if (!event!!.subheader.isNullOrEmpty()) {
                        binding.tvSubheader.visibility = View.VISIBLE
                        binding.tvSubheader.text = event!!.subheader
                    } else {
                        binding.tvSubheader.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@EventDetailActivity, getString(R.string.error_loading_event_details), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Error loading event details: ${e.message}")
                Toast.makeText(this@EventDetailActivity, getString(R.string.error, e.message), Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }


    private fun registerViewIfNeeded() {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        val viewPrefs = getSharedPreferences("views", Context.MODE_PRIVATE)
        val viewKey = "viewed_event_$eventId"

        // Если уже зарегистрирован просмотр, выходим из метода
        if (viewPrefs.getBoolean(viewKey, false)) {
            Log.d("EventDetail", "View already registered for eventId=$eventId")
            return
        }

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { eventsRepository.addView(eventId) }
                if (response.isSuccessful && response.body() != null) {
                    binding.tvViews.text = getString(R.string.views_count, response.body()!!.viewsCount)
                    // Сохраняем факт регистрации просмотра
                    viewPrefs.edit().putBoolean(viewKey, true).apply()
                    Log.d("EventDetail", "View registered for eventId=$eventId, userId=$userId")
                } else {
                    binding.tvViews.text = getString(R.string.views_count, event?.viewsCount ?: 0)
                    Log.d("EventDetail", "Failed to register view, using cached count for eventId=$eventId")
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Error registering view: ${e.message}")
                binding.tvViews.text = getString(R.string.views_count, event?.viewsCount ?: 0)
            }
        }
    }


    private fun loadLatestComment() {
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { commentRepository.getComments("event", eventId, 1) }
                if (response.isSuccessful && response.body() != null) {
                    val paginatedResponse = response.body()!!
                    totalComments = paginatedResponse.count
                    if (paginatedResponse.results.isNotEmpty()) {
                        val latestComment = paginatedResponse.results.first()
                        flatCommentAdapter.updateData(listOf(FlatComment(latestComment, 0)))
                        binding.tvCommentsHeader.text = getString(R.string.comments_header, totalComments)
                        binding.tvEmptyComments.visibility = View.GONE
                        binding.rvCommentsPreview.visibility = View.VISIBLE
                    } else {
                        flatCommentAdapter.updateData(emptyList())
                        binding.tvCommentsHeader.text = getString(R.string.write_comment)
                        binding.tvEmptyComments.visibility = View.VISIBLE
                        binding.rvCommentsPreview.visibility = View.GONE
                    }
                } else {
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

        val fragment = if (userId == currentUserId) {
            ProfileFragment()
        } else {
            OtherProfileFragment().apply {
                arguments = Bundle().apply { putInt("userId", userId) }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        binding.fragmentContainer.visibility = View.VISIBLE
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
        totalComments++
        binding.tvCommentsHeader.text = getString(R.string.comments_header, totalComments)
        binding.tvEmptyComments.visibility = View.GONE
        binding.rvCommentsPreview.visibility = View.VISIBLE
        flatCommentAdapter.updateData(listOf(FlatComment(newComment, 0)))
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