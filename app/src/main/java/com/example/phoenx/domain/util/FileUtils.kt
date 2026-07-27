package com.example.phoenx.domain.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object FileUtils {
    fun getTempImageUri(context: Context): Uri {
        val tempFile = File(context.cacheDir, "temp_image_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}
