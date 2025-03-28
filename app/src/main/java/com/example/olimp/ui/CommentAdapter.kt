package com.example.olimp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.olimp.R
import com.example.olimp.data.models.Comment

class CommentAdapter(
    private var comments: MutableList<Comment>,
    private val listener: CommentActionListener? = null
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    interface CommentActionListener {
        fun onLikeClicked(comment: Comment, position: Int)
        fun onEditClicked(comment: Comment, position: Int)
        fun onDeleteClicked(comment: Comment, position: Int)
    }

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val content: TextView = view.findViewById(R.id.tvCommentContent)
        val author: TextView = view.findViewById(R.id.tvCommentAuthor)
        val createdAt: TextView = view.findViewById(R.id.tvCommentCreatedAt)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.content.text = comment.content
        holder.author.text = comment.user?.username ?: "Аноним"
        holder.createdAt.text = formatDate(comment.createdAt)

        // Устанавливаем обработку кликов
        holder.btnLike.setOnClickListener {
            listener?.onLikeClicked(comment, position)
        }
        holder.btnEdit.setOnClickListener {
            listener?.onEditClicked(comment, position)
        }
        holder.btnDelete.setOnClickListener {
            listener?.onDeleteClicked(comment, position)
        }
    }

    override fun getItemCount(): Int = comments.size

    // Добавление нового комментария
    fun addComment(newComment: Comment) {
        comments.add(newComment)
        notifyItemInserted(comments.size - 1)
    }

    // Удаление комментария
    fun removeComment(position: Int) {
        if (position in comments.indices) {
            comments.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Обновление конкретного комментария
    fun updateComment(position: Int, updatedComment: Comment) {
        if (position in comments.indices) {
            comments[position] = updatedComment
            notifyItemChanged(position)
        }
    }

    // Обновление данных через DiffUtil
    fun updateData(newComments: List<Comment>) {
        val diffCallback = CommentDiffCallback(comments, newComments)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        comments.clear()
        comments.addAll(newComments)
        diffResult.dispatchUpdatesTo(this)
    }

    // Метод для форматирования даты
    private fun formatDate(isoDate: String?): String {
        return isoDate?.replace("T", " ")?.substring(0, 19) ?: "Неизвестная дата"
    }

    class CommentDiffCallback(
        private val oldList: List<Comment>,
        private val newList: List<Comment>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
