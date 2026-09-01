package com.example.phoenx.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    /**
     * Compresse et redimensionne une image pour optimiser le stockage et la fluidité.
     * Cible ~1080px de large/haut, qualité JPEG 80%.
     */
    fun compressAndResize(context: Context, uri: Uri, targetWidth: Int = 1080): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            // 1. Gestion de la rotation (Exif)
            val rotation = getRotation(context, uri)
            val rotatedBitmap = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            } else originalBitmap

            // 2. Calcul du redimensionnement (Proportionnel)
            val ratio = rotatedBitmap.width.toFloat() / rotatedBitmap.height.toFloat()
            val finalWidth: Int
            val finalHeight: Int

            if (rotatedBitmap.width > targetWidth || rotatedBitmap.height > targetWidth) {
                if (ratio > 1) {
                    finalWidth = targetWidth
                    finalHeight = (targetWidth / ratio).toInt()
                } else {
                    finalHeight = targetWidth
                    finalWidth = (targetWidth * ratio).toInt()
                }
            } else {
                finalWidth = rotatedBitmap.width
                finalHeight = rotatedBitmap.height
            }

            val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, finalWidth, finalHeight, true)

            // 3. Sauvegarde dans un fichier temporaire
            val tempFile = File(context.cacheDir, "PHX_IMG_${UUID.randomUUID()}.jpg")
            FileOutputStream(tempFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            // Libération mémoire
            if (scaledBitmap != rotatedBitmap) scaledBitmap.recycle()
            if (rotatedBitmap != originalBitmap) rotatedBitmap.recycle()
            originalBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "Erreur compression image", e)
            null
        }
    }

    private fun getRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) { 0 }
    }
}
