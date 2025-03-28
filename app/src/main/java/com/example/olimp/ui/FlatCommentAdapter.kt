package com.example.olimp.ui

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.olimp.R
import com.example.olimp.data.models.FlatComment
import com.example.olimp.data.models.Comment
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class FlatCommentAdapter(
    private var flatComments: MutableList<FlatComment>,
    private val onAction: (FlatComment, String) -> Unit
) : RecyclerView.Adapter<FlatCommentAdapter.FlatCommentViewHolder>() {

    private val dateCache = mutableMapOf<String, String>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    class FlatCommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val contentContainer: View = view.findViewById(R.id.contentContainer)
        val indentLine: View = view.findViewById(R.id.indentLine)
        val ivReplyIcon: ImageView = view.findViewById(R.id.ivReplyIcon)
        val tvReplyTo: TextView = view.findViewById(R.id.tvReplyTo)
        val tvCommentContent: TextView = view.findViewById(R.id.tvCommentContent)
        val tvCommentAuthor: TextView = view.findViewById(R.id.tvCommentAuthor)
        val tvCommentCreatedAt: TextView = view.findViewById(R.id.tvCommentCreatedAt)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
        val btnReply: ImageButton = view.findViewById(R.id.btnReply)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnShowReplies: Button = view.findViewById(R.id.btnShowReplies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlatCommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return FlatCommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlatCommentViewHolder, position: Int) {
        val flatComment = flatComments[position]
        val comment = flatComment.comment
        val context = holder.itemView.context

        Log.d(
            "FlatCommentAdapter",
            "Binding comment at pos=$position: id=${comment.id}, depth=${flatComment.depth}, isPlaceholder=${flatComment.isPlaceholder}, isExpanded=${flatComment.isExpanded}"
        )

        if (flatComment.isPlaceholder) {
            holder.ivAvatar.visibility = View.GONE
            holder.tvReplyTo.visibility = View.GONE
            holder.ivReplyIcon.visibility = View.GONE
            holder.tvCommentContent.visibility = View.GONE
            holder.tvCommentAuthor.visibility = View.GONE
            holder.tvCommentCreatedAt.visibility = View.GONE
            holder.btnLike.visibility = View.GONE
            holder.tvLikesCount.visibility = View.GONE
            holder.btnReply.visibility = View.GONE
            holder.btnEdit.visibility = View.GONE
            holder.btnDelete.visibility = View.GONE
            holder.btnShowReplies.visibility = View.VISIBLE

            if (flatComment.isExpanded) {
                holder.btnShowReplies.text = "Свернуть"
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_arrow_up)
                drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                holder.btnShowReplies.setCompoundDrawablesRelative(null, null, drawable, null)
            } else {
                holder.btnShowReplies.text = "Показать ответы (${flatComment.hiddenCount})"
                val drawable = ContextCompat.getDrawable(context, R.drawable.ic_arrow_down)
                drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                holder.btnShowReplies.setCompoundDrawablesRelative(null, null, drawable, null)
            }
            holder.btnShowReplies.setOnClickListener {
                Log.d("FlatCommentAdapter", "Clicked placeholder at pos=$position, action=${if (flatComment.isExpanded) "collapse" else "expand"}")
                onAction(flatComment, if (flatComment.isExpanded) "collapse" else "expand")
            }
            return
        }

        holder.btnShowReplies.visibility = View.GONE
        holder.ivAvatar.visibility = View.VISIBLE
        holder.tvCommentContent.visibility = View.VISIBLE
        holder.tvCommentAuthor.visibility = View.VISIBLE
        holder.tvCommentCreatedAt.visibility = View.VISIBLE
        holder.btnLike.visibility = View.VISIBLE
        holder.btnReply.visibility = View.VISIBLE

        if (flatComment.depth > 0) {
            holder.tvReplyTo.visibility = View.VISIBLE
            holder.ivReplyIcon.visibility = View.VISIBLE
            val replyToUsername = comment.replyTo ?: "unknown"
            val replyText = "Ответ на @$replyToUsername"
            val spannable = SpannableString(replyText)
            val atIndex = replyText.indexOf("@")
            if (atIndex >= 0) {
                val usernameColor = ContextCompat.getColor(context, R.color.red)
                spannable.setSpan(
                    ForegroundColorSpan(usernameColor),
                    atIndex,
                    replyText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            holder.tvReplyTo.text = spannable

            holder.indentLine.visibility = View.VISIBLE
            val lineIndent = flatComment.depth * 24
            holder.indentLine.setPadding(lineIndent, 0, 0, 0)

            holder.contentContainer.backgroundTintList =
                ContextCompat.getColorStateList(context, R.color.gray_light)
            holder.tvCommentContent.textSize = 12f
            holder.tvCommentAuthor.textSize = 10f
        } else {
            holder.tvReplyTo.visibility = View.GONE
            holder.ivReplyIcon.visibility = View.GONE
            holder.indentLine.visibility = View.GONE
            holder.contentContainer.backgroundTintList = null
            holder.tvCommentContent.textSize = 14f
            holder.tvCommentAuthor.textSize = 12f
        }

        holder.tvCommentContent.text = comment.content ?: "Нет текста"
        holder.tvCommentAuthor.text = comment.user?.username ?: "Аноним"

        val isoDate = comment.createdAt
        if (isoDate != null) {
            val formattedDate = dateCache[isoDate] ?: formatDate(isoDate).also { dateCache[isoDate] = it }
            holder.tvCommentCreatedAt.text = formattedDate
        } else {
            holder.tvCommentCreatedAt.text = "Неизвестная дата"
        }

        val likesCount = comment.likesCount ?: 0
        holder.tvLikesCount.text = likesCount.toString()
        holder.tvLikesCount.visibility = if (likesCount > 0) View.VISIBLE else View.GONE

        val isLiked = comment.isLiked ?: false
        holder.btnLike.setImageResource(
            if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
        )
        holder.btnLike.setColorFilter(
            ContextCompat.getColor(context, if (isLiked) R.color.red else R.color.gray)
        )

        val avatarUrl = comment.user?.avatar?.let {
            if (it.startsWith("http")) it else "http://10.0.2.2:8000$it"
        }
        Glide.with(context)
            .load(avatarUrl)
            .placeholder(R.drawable.ic_user_avatar)
            .error(R.drawable.ic_user_avatar)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .thumbnail(0.25f)
            .dontAnimate()
            .into(holder.ivAvatar)

        val indent = flatComment.depth * 48
        holder.contentContainer.setPadding(indent, 0, 0, 0)

        holder.ivAvatar.setOnClickListener { onAction(flatComment, "profile") }
        holder.btnLike.setOnClickListener { onAction(flatComment, "like") }
        holder.btnDelete.setOnClickListener { onAction(flatComment, "delete") }
        holder.btnReply.setOnClickListener { onAction(flatComment, "reply") }
        holder.btnEdit.setOnClickListener { onAction(flatComment, "edit") }

        val currentUserId = getCurrentUserId(context)
        val isAuthor = comment.user?.id == currentUserId
        holder.btnEdit.visibility = if (isAuthor) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (isAuthor) View.VISIBLE else View.GONE

        Log.d(
            "FlatCommentAdapter",
            "Position $position: isAuthor=$isAuthor, currentUserId=$currentUserId, commentUserId=${comment.user?.id}"
        )
    }

    override fun getItemCount(): Int {
        Log.d("FlatCommentAdapter", "getItemCount: ${flatComments.size}")
        return flatComments.size
    }

    fun getCurrentList(): MutableList<FlatComment> = flatComments

    fun updateData(newFlatComments: List<FlatComment>) {
        Log.d("FlatCommentAdapter", "Updating data: old size=${flatComments.size}, new size=${newFlatComments.size}")
        val diffCallback = FlatCommentDiffCallback(flatComments, newFlatComments)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        val updatedList = mutableListOf<FlatComment>().apply { addAll(newFlatComments) }
        flatComments = updatedList
        diffResult.dispatchUpdatesTo(this)
        Log.d("FlatCommentAdapter", "Data updated, new size=${flatComments.size}")
    }

    fun replaceComment(updated: Comment) {
        val index = flatComments.indexOfFirst { it.comment.id == updated.id }
        if (index != -1) {
            val updatedItem = flatComments[index].copy(comment = updated)
            flatComments[index] = updatedItem
            notifyItemChanged(index)
            Log.d("FlatCommentAdapter", "🔄 Replaced comment at index=$index with id=${updated.id}")
        } else {
            Log.w("FlatCommentAdapter", "⚠️ Comment to replace not found: id=${updated.id}")
        }
    }

    private fun formatDate(isoDate: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = isoFormat.parse(isoDate)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("FlatCommentAdapter", "Date parsing error: $isoDate, ${e.message}")
            isoDate.replace("T", " ").substring(0, 19)
        }
    }

    private fun getCurrentUserId(context: Context): Int? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1).takeIf { it != -1 }
        Log.d("FlatCommentAdapter", "Current user ID: $userId")
        return userId
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        coroutineScope.cancel()
    }
}

private class FlatCommentDiffCallback(
    private val oldList: List<FlatComment>,
    private val newList: List<FlatComment>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        val result = if (oldItem.isPlaceholder && newItem.isPlaceholder) {
            oldItem.comment.id == newItem.comment.id && oldItem.depth == newItem.depth
        } else {
            oldItem.comment.id == newItem.comment.id && oldItem.isPlaceholder == newItem.isPlaceholder
        }
        Log.d("FlatCommentAdapter", "areItemsTheSame: oldPos=$oldItemPosition, newPos=$newItemPosition, result=$result")
        return result
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val result = oldList[oldItemPosition] == newList[newItemPosition]
        Log.d("FlatCommentAdapter", "areContentsTheSame: oldPos=$oldItemPosition, newPos=$newItemPosition, result=$result")
        return result
    }
}