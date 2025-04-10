package com.example.olimp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.olimp.R
import com.example.olimp.data.models.*
import com.example.olimp.data.repository.CommentRepository
import com.example.olimp.data.repository.NewsRepository
import com.example.olimp.databinding.ActivityNewsDetailBinding
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewsDetailActivity : AppCompatActivity(),
    CommentsBottomSheetDialogFragment.CommentUpdateListener {

    private lateinit var binding: ActivityNewsDetailBinding
    private lateinit var newsRepository: NewsRepository
    private lateinit var commentRepository: CommentRepository
    private lateinit var flatCommentAdapter: FlatCommentAdapter

    private var newsId: Int = 0
    private lateinit var news: News
    private var likeCount: Int = 0
    private var isLiked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация репозиториев
        newsRepository = NewsRepository(this)
        commentRepository = CommentRepository(RetrofitInstance.getApi(this))

        // Инициализация RecyclerView для превью комментариев
        binding.rvCommentsPreview.layoutManager = LinearLayoutManager(this)
        flatCommentAdapter = FlatCommentAdapter(mutableListOf()) { flatComment, action ->
            when (action) {
                "profile" -> goToProfile(flatComment)
                "like" -> toggleLike(flatComment)
                "delete" -> deleteComment(flatComment)
                "reply" -> showCommentsBottomSheetWithReply(flatComment.comment.id)
                "edit" -> {} // Редактирование не поддерживается в превью
                "expand" -> {} // Не используется в превью
                "collapse" -> {} // Не используется в превью
            }
        }
        binding.rvCommentsPreview.adapter = flatCommentAdapter

        // Открытие BottomSheet при клике на превью или заголовок
        binding.rvCommentsPreview.setOnClickListener { showCommentsBottomSheet() }
        binding.tvCommentsHeader.setOnClickListener { showCommentsBottomSheet() }

        // Открытие BottomSheet при клике на "Напишите комментарий..."
        binding.llCommentInputPreview.setOnClickListener {
            showCommentsBottomSheet()
        }

        // Получаем ID новости из Intent
        newsId = intent.getIntExtra("NEWS_ID", 0)
        if (newsId == 0) {
            Toast.makeText(this, "Ошибка: неверный ID новости", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Загрузка данных новости и последнего комментария
        loadNewsDetail()
        loadLatestComment()

        // Обработчик клика для лайка новости
        binding.btnLike.setOnClickListener { addLike() }
    }

    override fun onResume() {
        super.onResume()
        loadLatestComment() // Обновляем превью при возвращении
    }

    override fun onCommentAdded(newComment: Comment) {
        val currentList = flatCommentAdapter.getCurrentList()
        val exists = currentList.any { it.comment.id == newComment.id }
        val newList = if (exists) {
            currentList
        } else {
            listOf(FlatComment(newComment, 0)) // Превью показывает только один комментарий
        }
        flatCommentAdapter.updateData(newList)
        binding.tvCommentsHeader.text = "Комментарии"
    }

    override fun onCommentUpdated(updatedComment: Comment, position: Int) {
        val currentList = flatCommentAdapter.getCurrentList()
        val index = currentList.indexOfFirst { it.comment.id == updatedComment.id }
        if (index != -1) {
            val newList = currentList.toMutableList().apply {
                this[index] = this[index].copy(comment = updatedComment)
            }
            flatCommentAdapter.updateData(newList)
        }
    }

    override fun onCommentDeleted(position: Int) {
        val currentList = flatCommentAdapter.getCurrentList()
        if (position in currentList.indices) {
            val newList = currentList.toMutableList().apply {
                removeAt(position)
            }
            flatCommentAdapter.updateData(newList)
            if (newList.isEmpty()) {
                binding.tvCommentsHeader.text = "Напишите комментарий"
            }
        }
        loadLatestComment() // Обновляем превью
    }

    override fun onCommentLiked(updatedComment: Comment, position: Int) {
        val currentList = flatCommentAdapter.getCurrentList()
        if (position in currentList.indices) {
            val newList = currentList.toMutableList().apply {
                this[position] = this[position].copy(comment = updatedComment)
            }
            flatCommentAdapter.updateData(newList)
        }
    }

    private fun loadNewsDetail() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { newsRepository.getNewsDetail(newsId) }
                if (response.isSuccessful && response.body() != null) {
                    news = response.body()!!
                    bindNews(news)
                    registerViewIfNeeded(newsId)
                } else {
                    Toast.makeText(this@NewsDetailActivity, "Ошибка загрузки новости: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("NewsDetail", "Error loading news: ${e.message}")
                Toast.makeText(this@NewsDetailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindNews(news: News) {
        binding.tvTitle.text = news.title

        val fullText = news.fullText ?: ""
        val subheader = intent.getStringExtra("NEWS_SUBHEADER")

        binding.tvSubheader.visibility = if (subheader.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.tvSubheader.text = subheader

        // Показываем просмотры
        binding.tvViews.text = news.viewsCount.toString()

        // Сохраняем и применяем лайк
        likeCount = news.likes
        isLiked = news.isLiked
        binding.tvLikes.text = likeCount.toString()
        updateLikeButton() // Обновляем кнопку после isLiked!

        val photos = news.photos ?: emptyList()
        if (photos.isNotEmpty()) {
            val normalizedPhotos = photos.map { photo ->
                val originalUrl = photo.photo
                val fullUrl = if (originalUrl.startsWith("http")) {
                    originalUrl // Не добавляем префикс, если URL уже полный
                } else if (originalUrl.startsWith("/")) {
                    "http://10.0.2.2:8000$originalUrl" // Добавляем только если относительный путь с "/"
                } else {
                    "http://10.0.2.2:8000/media/news/$originalUrl" // Добавляем если просто имя файла
                }
                photo.copy(photo = fullUrl)
            }
            val adapter = NewsPhotoPagerAdapter(normalizedPhotos)
            binding.viewPagerPhotos.adapter = adapter
            binding.viewPagerPhotos.visibility = View.VISIBLE
            binding.indicator.setViewPager(binding.viewPagerPhotos)
            binding.indicator.visibility = View.VISIBLE
        } else {
            binding.viewPagerPhotos.visibility = View.GONE
            binding.indicator.visibility = View.GONE
        }


        // --- Логика сворачивания текста ---
        val isTextLong = fullText.length > 200
        val maxLinesCollapsed = 4
        var isExpanded = false

        binding.tvFullContent.text = fullText
        binding.tvFullContent.maxLines = if (isTextLong) maxLinesCollapsed else Int.MAX_VALUE
        binding.tvFullContent.ellipsize = if (isTextLong) TextUtils.TruncateAt.END else null
        binding.btnToggleContent.visibility = if (isTextLong) View.VISIBLE else View.GONE

        binding.btnToggleContent.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                binding.tvFullContent.maxLines = Int.MAX_VALUE
                binding.tvFullContent.ellipsize = null
                binding.btnToggleContent.text = "Свернуть"
            } else {
                binding.tvFullContent.maxLines = maxLinesCollapsed
                binding.tvFullContent.ellipsize = TextUtils.TruncateAt.END
                binding.btnToggleContent.text = "Показать полностью"
            }
        }
    }



    private fun addLike() {
        if (!SessionManager(this).isUserLoggedIn()) {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { newsRepository.addLike(newsId) }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        likeCount = body.likes
                        isLiked = body.isLiked
                        binding.tvLikes.text = "Лайки: $likeCount"
                        updateLikeButton()
                        Toast.makeText(
                            this@NewsDetailActivity,
                            if (isLiked) "Лайк добавлен!" else "Лайк удалён!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(this@NewsDetailActivity, "Ошибка: пустой ответ от сервера", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Log.e("NewsDetail", "Error adding like: ${e.message}")
                Toast.makeText(this@NewsDetailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLikeButton() {
        val icon = if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
        binding.btnLike.setImageResource(icon)
        binding.btnLike.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(200)
            .withEndAction {
                binding.btnLike.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            .start()
    }

    private fun loadLatestComment() {
        lifecycleScope.launch {
            try {
                binding.progressBarComments.visibility = View.VISIBLE
                val response = withContext(Dispatchers.IO) { commentRepository.getComments("news", newsId, page = 1) }
                if (response.isSuccessful) {
                    val paginatedResponse = response.body()
                    if (paginatedResponse != null && paginatedResponse.results.isNotEmpty()) {
                        val latestComment = paginatedResponse.results.first()
                        flatCommentAdapter.updateData(listOf(FlatComment(latestComment, 0)))
                        binding.tvCommentsHeader.text = "Комментарии"
                    } else {
                        flatCommentAdapter.updateData(emptyList())
                        binding.tvCommentsHeader.text = "Напишите комментарий"
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Log.e("NewsDetail", "Error loading latest comment: ${e.message}")
                Toast.makeText(this@NewsDetailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                flatCommentAdapter.updateData(emptyList())
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun goToProfile(flatComment: FlatComment) {
        val targetUserId = flatComment.comment.user?.id
        if (targetUserId == null) {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }
        if (!SessionManager(this).isUserLoggedIn()) {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        Log.d("NewsDetailActivity", "goToProfile called with targetUserId=$targetUserId")
        val intent = Intent(this, ProfileHostActivity::class.java)
        intent.putExtra("userId", targetUserId)
        startActivity(intent)
    }

    private fun toggleLike(flatComment: FlatComment) {
        if (!SessionManager(this).isUserLoggedIn()) {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { commentRepository.toggleCommentLike(flatComment.comment.id) }
                if (response.isSuccessful) {
                    val likeToggleResponse = response.body()
                    if (likeToggleResponse != null) {
                        val updatedComment = flatComment.comment.copy(
                            likesCount = likeToggleResponse.likes_count,
                            isLiked = likeToggleResponse.liked
                        )
                        val currentList = flatCommentAdapter.getCurrentList()
                        val index = currentList.indexOf(flatComment)
                        if (index != -1) {
                            val newList = currentList.toMutableList().apply {
                                this[index] = this[index].copy(comment = updatedComment)
                            }
                            flatCommentAdapter.updateData(newList)
                        }
                        Toast.makeText(this@NewsDetailActivity, "Лайк обновлён", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Log.e("NewsDetail", "Error toggling like: ${e.message}")
                Toast.makeText(this@NewsDetailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun deleteComment(flatComment: FlatComment) {
        if (!SessionManager(this).isUserLoggedIn()) {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }
        lifecycleScope.launch {
            binding.progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) { commentRepository.deleteComment(flatComment.comment.id) }
                if (response.isSuccessful) {
                    val currentList = flatCommentAdapter.getCurrentList()
                    val index = currentList.indexOf(flatComment)
                    if (index != -1) {
                        val newList = currentList.toMutableList().apply {
                            removeAt(index)
                        }
                        flatCommentAdapter.updateData(newList)
                        if (newList.isEmpty()) {
                            binding.tvCommentsHeader.text = "Напишите комментарий"
                        }
                    }
                    loadLatestComment() // Обновляем превью
                    Toast.makeText(this@NewsDetailActivity, "Комментарий удалён", Toast.LENGTH_SHORT).show()
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Log.e("NewsDetail", "Error deleting comment: ${e.message}")
                Toast.makeText(this@NewsDetailActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun registerViewIfNeeded(newsId: Int) {
        val prefs = getSharedPreferences("viewed_news", Context.MODE_PRIVATE)
        val viewedNews = prefs.getStringSet("viewed", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (viewedNews.contains(newsId.toString())) {
            Log.d("NewsDetail", "View already registered for newsId: $newsId")
            return
        }
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { newsRepository.addView(newsId) }
                if (response.isSuccessful) {
                    viewedNews.add(newsId.toString())
                    prefs.edit().putStringSet("viewed", viewedNews).apply()
                    val body = response.body()
                    if (body != null) {
                        binding.tvViews.text = "Просмотры: ${body.viewsCount}"
                    } else {
                        binding.tvViews.text = "Просмотры: ${news.viewsCount + 1}"
                    }
                }
            } catch (e: Exception) {
                Log.e("NewsDetail", "Error registering view: ${e.message}")
            }
        }
    }

    private fun showCommentsBottomSheet() {
        val bottomSheet = CommentsBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putInt("entityId", newsId)
                putString("entityType", "news")
            }
        }
        bottomSheet.show(supportFragmentManager, "CommentsBottomSheet")
    }

    private fun showCommentsBottomSheetWithReply(parentCommentId: Int) {
        val bottomSheet = CommentsBottomSheetDialogFragment().apply {
            arguments = Bundle().apply {
                putInt("entityId", newsId)
                putString("entityType", "news")
                putInt("parentCommentId", parentCommentId)
            }
        }
        bottomSheet.show(supportFragmentManager, "CommentsBottomSheet")
    }

    private fun handleApiError(code: Int) {
        Toast.makeText(this@NewsDetailActivity, "Ошибка: $code", Toast.LENGTH_SHORT).show()
        if (code == 401) {
            SessionManager(this@NewsDetailActivity).clearAuthToken()
            startActivity(Intent(this@NewsDetailActivity, LoginActivity::class.java))
            finish()
        }
    }
}