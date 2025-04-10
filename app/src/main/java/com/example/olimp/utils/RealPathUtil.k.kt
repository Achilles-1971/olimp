package com.example.olimp.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

object RealPathUtil {
    fun getRealPath(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            return it.getString(columnIndex)
        }
        return null
    }
}
