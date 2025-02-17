package com.example.olimp.ui

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.olimp.R
import com.example.olimp.network.RetrofitInstance
import com.example.olimp.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var avatarImageView: ImageView
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var bioEdit: EditText
    private lateinit var updateButton: Button
    private lateinit var changeAvatarButton: Button

    private lateinit var sessionManager: SessionManager
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)

        avatarImageView = findViewById(R.id.avatarImageView)
        usernameText = findViewById(R.id.usernameText)
        emailText = findViewById(R.id.emailText)
        bioEdit = findViewById(R.id.bioEdit)
        updateButton = findViewById(R.id.updateButton)
        changeAvatarButton = findViewById(R.id.changeAvatarButton)

        loadUserProfile()

        changeAvatarButton.setOnClickListener {
            openGallery()
        }

        updateButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun loadUserProfile() {
        usernameText.text = sessionManager.getUsername()
        emailText.text = sessionManager.getEmail()
        bioEdit.setText(sessionManager.getBio())

        val avatarUrl = sessionManager.getAvatarUrl()
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(avatarImageView)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            avatarImageView.setImageURI(imageUri)
        }
    }

    private fun updateProfile() {
        val bio = bioEdit.text.toString()
        val userId = sessionManager.getUserId()

        lifecycleScope.launch {
            val bioPart = RequestBody.create("text/plain".toMediaTypeOrNull(), bio)

            val avatarPart = imageUri?.let { uri ->
                val filePath = getRealPathFromURI(uri)
                filePath?.let {
                    val file = File(it)
                    val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                    MultipartBody.Part.createFormData("avatar", file.name, requestFile)
                }
            }

            val response = RetrofitInstance.api.updateUserProfile(userId, bioPart, avatarPart)
            if (response.isSuccessful) {
                sessionManager.updateUserBio(bio)
                avatarPart?.let { sessionManager.updateUserAvatar(imageUri.toString()) }
                Toast.makeText(this@ProfileActivity, "Профиль обновлен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ProfileActivity, "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var result: String? = null
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                result = it.getString(columnIndex)
            }
        }
        return result
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1001
    }
}
