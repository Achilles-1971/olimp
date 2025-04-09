package com.example.olimp.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.R
import com.example.olimp.data.models.Comment
import com.example.olimp.data.models.CommentRequest
import com.example.olimp.data.models.FlatComment
import com.example.olimp.data.repository.CommentRepository
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.*

class CommentsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etCommentInput: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var progressBarComments: View
    private lateinit var tvEmptyComments: TextView
    private lateinit var llCommentInput: View
    private lateinit var flatCommentAdapter: FlatCommentAdapter

    // Используем CommentRepository для работы с комментариями
    private lateinit var commentRepository: CommentRepository

    // Параметры, которые будем получать через arguments
    private var entityId: Int = 0
    private var entityType: String = "news"

    private var replyingToCommentId: Int? = null
    private var currentPage = 1
    private var totalComments = 0

    private var commentUpdateListener: CommentUpdateListener? = null
    private var fullComments: List<Comment> = emptyList()

    interface CommentUpdateListener {
        fun onCommentAdded(newComment: Comment)
        fun onCommentUpdated(updatedComment: Comment, position: Int)
        fun onCommentDeleted(position: Int)
        fun onCommentLiked(updatedComment: Comment, position: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        commentUpdateListener = try {
            context as CommentUpdateListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement CommentUpdateListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        commentUpdateListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entityId = arguments?.getInt("entityId") ?: 0
        entityType = arguments?.getString("entityType") ?: "news"

        // Инициализируем CommentRepository
        commentRepository = CommentRepository(RetrofitInstance.getApi(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false)

        recyclerView = view.findViewById(R.id.rvCommentsBottomSheet)
        etCommentInput = view.findViewById(R.id.etCommentInput)
        btnSendComment = view.findViewById(R.id.btnSendComment)
        btnClose = view.findViewById(R.id.btnClose)
        progressBarComments = view.findViewById(R.id.progressBarComments)
        tvEmptyComments = view.findViewById(R.id.tvEmptyComments)
        llCommentInput = view.findViewById(R.id.llCommentInput)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)
        flatCommentAdapter = FlatCommentAdapter(mutableListOf()) { flatComment, action ->
            when (action) {
                "profile" -> openUserProfile(flatComment.comment.user?.id)
                "like" -> toggleCommentLike(flatComment, flatCommentAdapter.getCurrentList().indexOf(flatComment))
                "delete" -> deleteComment(flatComment, flatCommentAdapter.getCurrentList().indexOf(flatComment))
                "reply" -> {
                    replyingToCommentId = flatComment.comment.id
                    etCommentInput.requestFocus()
                    updateCommentInputHint()
                }
                "edit" -> openEditDialog(flatComment, flatCommentAdapter.getCurrentList().indexOf(flatComment))
                "expand" -> expandPlaceholder(flatComment)
                "collapse" -> collapsePlaceholder(flatComment)
            }
        }
        recyclerView.adapter = flatCommentAdapter

        llCommentInput.background = ContextCompat.getDrawable(requireContext(), R.drawable.comment_input_background)
        llCommentInput.clipToOutline = true

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val layoutManager = rv.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (totalItemCount < totalComments && lastVisibleItem >= totalItemCount - 5 && dy > 0) {
                    loadMoreComments()
                }
            }
        })

        updateCommentInputHint()

        btnSendComment.setOnClickListener { sendComment() }
        etCommentInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment()
                true
            } else {
                false
            }
        }

        btnClose.setOnClickListener { dismiss() }

        loadComments()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.let { bsd ->
            bsd.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
                bottomSheet.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_nav_background)
                bottomSheet.clipToOutline = true

                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.isDraggable = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isFitToContents = false
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(sheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss()
                        }
                    }
                    override fun onSlide(sheet: View, offset: Float) {}
                })

                val screenHeight = resources.displayMetrics.heightPixels
                behavior.peekHeight = screenHeight
                val lp = bottomSheet.layoutParams
                lp.height = screenHeight
                bottomSheet.layoutParams = lp
            }
        }
    }

    private fun updateCommentInputHint() {
        if (replyingToCommentId == null) {
            etCommentInput.hint = "Напишите комментарий..."
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                val username = findCommentById(fullComments, replyingToCommentId!!)?.user?.username ?: "unknown"
                etCommentInput.hint = "Ответить @$username..."
            }
        }
    }

    private fun findCommentById(comments: List<Comment>, commentId: Int): Comment? {
        for (c in comments) {
            if (c.id == commentId) return c
            val found = findCommentById(c.children, commentId)
            if (found != null) return found
        }
        return null
    }

    private fun flattenComments(comments: List<Comment>, depth: Int = 0): List<FlatComment> {
        val result = mutableListOf<FlatComment>()
        for (comment in comments) {
            result.add(FlatComment(comment, depth))
            Log.d("CommentsBottomSheet", "Flatten: Added comment id=${comment.id}, depth=$depth")

            if (comment.children.isNotEmpty()) {
                if (!isExpanded(comment.id)) {
                    val hiddenCount = comment.children.size
                    val placeholder = Comment(
                        id = -comment.id,
                        user = null,
                        content = "Показать ответы ($hiddenCount)",
                        parentCommentId = comment.id,
                        likesCount = 0,
                        isLiked = false,
                        userAvatar = null,
                        children = emptyList(),
                        createdAt = comment.createdAt,
                        replyTo = null
                    )
                    result.add(FlatComment(placeholder, depth + 1, isPlaceholder = true, hiddenCount = hiddenCount))
                    Log.d("CommentsBottomSheet", "Flatten: Added placeholder for comment id=${comment.id}, hiddenCount=$hiddenCount")
                } else {
                    result.addAll(flattenComments(comment.children, depth + 1))
                    val collapsePlaceholder = Comment(
                        id = -comment.id - 1,
                        user = null,
                        content = "Свернуть",
                        parentCommentId = comment.id,
                        likesCount = 0,
                        isLiked = false,
                        userAvatar = null,
                        children = emptyList(),
                        createdAt = comment.createdAt,
                        replyTo = null
                    )
                    result.add(FlatComment(collapsePlaceholder, depth + 1, isPlaceholder = true, isExpanded = true))
                    Log.d("CommentsBottomSheet", "Flatten: Added collapse placeholder for comment id=${comment.id}")
                }
            }
        }
        return result
    }

    private fun isExpanded(commentId: Int): Boolean {
        return flatCommentAdapter.getCurrentList().any {
            it.isPlaceholder && it.comment.parentCommentId == commentId && it.isExpanded
        }
    }

    private fun expandPlaceholder(placeholder: FlatComment) {
        val parentId = placeholder.comment.parentCommentId ?: return
        val parentComment = findCommentById(fullComments, parentId) ?: return

        val currentList = flatCommentAdapter.getCurrentList()
        val index = currentList.indexOfFirst { it.comment.id == placeholder.comment.id }
        if (index == -1) return

        val newList = mutableListOf<FlatComment>().apply {
            addAll(currentList.subList(0, index))
            val expandedChildren = flattenComments(parentComment.children, placeholder.depth)
            addAll(expandedChildren)
            val collapsePlaceholder = FlatComment(
                comment = Comment(
                    id = -parentId - 1,
                    user = null,
                    content = "Свернуть",
                    parentCommentId = parentId,
                    likesCount = 0,
                    isLiked = false,
                    userAvatar = null,
                    children = emptyList(),
                    createdAt = parentComment.createdAt,
                    replyTo = null
                ),
                depth = placeholder.depth,
                isPlaceholder = true,
                hiddenCount = 0,
                isExpanded = true
            )
            add(collapsePlaceholder)
            if (index + 1 < currentList.size) {
                addAll(currentList.subList(index + 1, currentList.size))
            }
        }

        flatCommentAdapter.updateData(newList)
        recyclerView.scrollToPosition(index)
        Log.d("CommentsBottomSheet", "Expanded: added ${parentComment.children.size} child comments + collapse placeholder.")
    }

    private fun collapsePlaceholder(placeholder: FlatComment) {
        val parentId = placeholder.comment.parentCommentId ?: return
        val currentList = flatCommentAdapter.getCurrentList()
        val collapseIndex = currentList.indexOfFirst { it.comment.id == placeholder.comment.id }
        if (collapseIndex == -1) return

        val parentIndex = currentList.indexOfFirst { it.comment.id == parentId }
        if (parentIndex == -1) return

        val parentComment = findCommentById(fullComments, parentId) ?: return
        val hiddenCount = parentComment.children.size

        val newList = mutableListOf<FlatComment>().apply {
            addAll(currentList.subList(0, parentIndex + 1))
            val showPlaceholder = FlatComment(
                comment = Comment(
                    id = -parentId,
                    user = null,
                    content = "Показать ответы ($hiddenCount)",
                    parentCommentId = parentId,
                    likesCount = 0,
                    isLiked = false,
                    userAvatar = null,
                    children = emptyList(),
                    createdAt = parentComment.createdAt,
                    replyTo = null
                ),
                depth = placeholder.depth,
                isPlaceholder = true,
                hiddenCount = hiddenCount,
                isExpanded = false
            )
            add(showPlaceholder)
            if (collapseIndex + 1 < currentList.size) {
                addAll(currentList.subList(collapseIndex + 1, currentList.size))
            }
        }

        flatCommentAdapter.updateData(newList)
        recyclerView.scrollToPosition(parentIndex)
        Log.d("CommentsBottomSheet", "Collapsed: removed child comments, added showPlaceholder.")
    }

    private var commentWasAdded = false

    private fun sendComment() {
        val content = etCommentInput.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Введите текст комментария", Toast.LENGTH_SHORT).show()
            return
        }

        if (!SessionManager(requireContext()).isUserLoggedIn()) {
            Toast.makeText(requireContext(), "Требуется авторизация", Toast.LENGTH_SHORT).show()
            startActivity(Intent(context, LoginActivity::class.java))
            return
        }

        val request = CommentRequest(
            entityId = entityId,
            content = content,
            entityType = entityType,
            parentCommentId = replyingToCommentId
        )

        CoroutineScope(Dispatchers.Main).launch {
            progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) {
                    commentRepository.createComment(request)
                }

                if (response.isSuccessful) {
                    val commentsList = response.body()
                    Log.d("CommentsBottomSheet", "Server response: $commentsList")

                    if (!commentsList.isNullOrEmpty()) {
                        val currentUserId = SessionManager(requireContext()).getUserId()
                        val newComment = commentsList.find {
                            it.content == content && it.user?.id == currentUserId
                        }

                        if (newComment != null) {
                            Log.d("CommentsBottomSheet", "New comment found: $newComment")
                            fullComments = updateCommentTreeSafe(fullComments, newComment)
                            flatCommentAdapter.updateData(flattenComments(fullComments))
                            etCommentInput.text.clear()
                            replyingToCommentId = null
                            updateCommentInputHint()
                            totalComments++
                            updateEmptyState()
                            Toast.makeText(requireContext(), "Комментарий отправлен", Toast.LENGTH_SHORT).show()
                            commentWasAdded = true
                            commentUpdateListener?.onCommentAdded(newComment)
                        } else {
                            Log.w("CommentsBottomSheet", "New comment not found in response, refreshing full list")
                            fullComments = commentsList
                            flatCommentAdapter.updateData(flattenComments(fullComments))
                            totalComments = commentsList.size
                            updateEmptyState()
                        }
                    } else {
                        Log.e("CommentsBottomSheet", "Response body is empty")
                        Toast.makeText(requireContext(), "Ошибка: ответ сервера пуст", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Log.e("CommentsBottomSheet", "Exception in sendComment: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBarComments.visibility = View.GONE
            }
        }
    }


    private fun updateCommentTreeSafe(comments: List<Comment>, newComment: Comment): List<Comment> {
        // если корневой — просто добавляем
        if (newComment.parentCommentId == null) {
            return listOf(newComment) + comments
        }

        // иначе рекурсивно вставляем в нужное место
        return comments.map { old ->
            when {
                old.id == newComment.id -> {
                    newComment.copy(children = updateCommentTreeSafe(old.children, newComment))
                }
                old.id == newComment.parentCommentId -> {
                    if (old.children.none { it.id == newComment.id }) {
                        old.copy(children = listOf(newComment) + old.children)
                    } else old
                }
                else -> {
                    old.copy(children = updateCommentTreeSafe(old.children, newComment))
                }
            }
        }
    }


    private var isLoading = false

    private fun loadComments(page: Int = 1) {
        if (isLoading) return
        isLoading = true

        CoroutineScope(Dispatchers.Main).launch {
            progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) {
                    commentRepository.getComments(entityType, entityId, page)
                }

                if (response.isSuccessful) {
                    val paginatedResponse = response.body()
                    if (paginatedResponse != null) {
                        fullComments = if (page == 1) {
                            paginatedResponse.results
                        } else {
                            fullComments + paginatedResponse.results
                        }

                        val flatList = flattenComments(fullComments)
                        flatCommentAdapter.updateData(flatList)

                        Log.d("CommentsLoader", "📦 Загружено ${flatList.size} комментариев (page=$page)")

                        totalComments = paginatedResponse.count
                        currentPage = page

                        updateEmptyState()
                        autoLoadMoreIfNeeded()
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Log.e("CommentsLoader", "❌ Ошибка загрузки: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка загрузки комментариев", Toast.LENGTH_SHORT).show()
            } finally {
                progressBarComments.visibility = View.GONE
                isLoading = false
            }
        }
    }


    private fun autoLoadMoreIfNeeded() {
        recyclerView.post {
            if (!recyclerView.canScrollVertically(1) && flatCommentAdapter.itemCount < totalComments) {
                loadComments(currentPage + 1)
            }
        }
    }

    private fun loadMoreComments() {
        if (flatCommentAdapter.itemCount < totalComments) {
            loadComments(currentPage + 1)
        }
    }

    private fun updateEmptyState() {
        val isEmpty = flatCommentAdapter.itemCount == 0
        tvEmptyComments.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        etCommentInput.visibility = View.VISIBLE
        btnSendComment.visibility = View.VISIBLE
    }

    private fun openUserProfile(userId: Int?) {
        dismiss()
        userId ?: run {
            Toast.makeText(requireContext(), "Нет данных пользователя", Toast.LENGTH_SHORT).show()
            return
        }
        val sessionManager = SessionManager(requireContext())
        val currentUserId = sessionManager.getUserId()

        val fragment = if (userId == currentUserId) {
            ProfileFragment()
        } else {
            OtherProfileFragment().apply {
                arguments = Bundle().apply { putInt("userId", userId) }
            }
        }
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragmentContainer, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun openEditDialog(flatComment: FlatComment, position: Int) {
        val editText = EditText(requireContext()).apply {
            setText(flatComment.comment.content)
            setSelection(flatComment.comment.content.length)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать комментарий")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateComment(flatComment, position, newText)
                }
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateComment(flatComment: FlatComment, position: Int, newText: String) {
        val request = CommentRequest(
            entityId = entityId,
            content = newText,
            entityType = entityType,
            parentCommentId = flatComment.comment.parentCommentId
        )
        CoroutineScope(Dispatchers.Main).launch {
            progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) {
                    commentRepository.updateComment(flatComment.comment.id, request)
                }
                if (response.isSuccessful) {
                    val updatedComment = response.body()
                    if (updatedComment != null) {
                        val currentList = flatCommentAdapter.getCurrentList()
                        val updatedList = currentList.mapIndexed { idx, item ->
                            if (!item.isPlaceholder && item.comment.id == updatedComment.id) {
                                item.copy(comment = updatedComment)
                            } else item
                        }
                        flatCommentAdapter.updateData(updatedList)
                        fullComments = updateCommentTreeSafe(fullComments, updatedComment)
                        Toast.makeText(requireContext(), "Комментарий обновлён", Toast.LENGTH_SHORT).show()
                        commentUpdateListener?.onCommentUpdated(updatedComment, position)
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun deleteComment(flatComment: FlatComment, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) {
                    commentRepository.deleteComment(flatComment.comment.id)
                }
                if (response.isSuccessful) {
                    fullComments = removeCommentFromTree(fullComments, flatComment.comment.id)
                    flatCommentAdapter.updateData(flattenComments(fullComments))
                    commentUpdateListener?.onCommentDeleted(position)
                    totalComments--
                    updateEmptyState()
                    Toast.makeText(requireContext(), "Комментарий удалён", Toast.LENGTH_SHORT).show()
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun removeCommentFromTree(comments: List<Comment>, commentId: Int): List<Comment> {
        return comments
            .filter { it.id != commentId }
            .map { it.copy(children = removeCommentFromTree(it.children, commentId)) }
    }

    private fun toggleCommentLike(flatComment: FlatComment, position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            progressBarComments.visibility = View.VISIBLE
            try {
                val response = withContext(Dispatchers.IO) {
                    commentRepository.toggleCommentLike(flatComment.comment.id)
                }
                if (response.isSuccessful) {
                    val toggleResponse = response.body()
                    if (toggleResponse != null) {
                        val updatedComment = flatComment.comment.copy(
                            likesCount = toggleResponse.likes_count,
                            isLiked = toggleResponse.liked
                        )
                        val currentList = flatCommentAdapter.getCurrentList()
                        val updatedList = currentList.mapIndexed { idx, item ->
                            if (!item.isPlaceholder && item.comment.id == updatedComment.id) {
                                item.copy(comment = updatedComment)
                            } else item
                        }
                        flatCommentAdapter.updateData(updatedList)
                        fullComments = updateCommentTreeSafe(fullComments, updatedComment)
                        commentUpdateListener?.onCommentLiked(updatedComment, position)
                    }
                } else {
                    handleApiError(response.code())
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBarComments.visibility = View.GONE
            }
        }
    }

    private fun handleApiError(code: Int) {
        Toast.makeText(requireContext(), "Ошибка: $code", Toast.LENGTH_SHORT).show()
        if (code == 401) {
            SessionManager(requireContext()).clearAuthToken()
            startActivity(Intent(context, LoginActivity::class.java))
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CoroutineScope(Dispatchers.Main).cancel()
    }
}