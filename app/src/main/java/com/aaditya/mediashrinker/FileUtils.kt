package com.aaditya.mediashrinker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

object FileUtils {

    fun getPath(
        context: Context,
        uri: Uri
    ): String {

        var result: String? = null

        val projection =
            arrayOf(
                MediaStore.Video.Media.DATA
            )

        val cursor: Cursor? =
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )

        cursor?.use {

            if (it.moveToFirst()) {

                val columnIndex =
                    it.getColumnIndexOrThrow(
                        MediaStore.Video.Media.DATA
                    )

                result =
                    it.getString(columnIndex)
            }
        }

        return result ?: ""
    }
}