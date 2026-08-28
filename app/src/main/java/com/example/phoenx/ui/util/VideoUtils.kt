package com.example.phoenx.ui.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoUtils {
    const val MAX_VIDEO_DURATION_SECONDS_STANDARD = 90
    const val MAX_VIDEO_DURATION_SECONDS_PACTE = 30

    /**
     * Vérifie si la durée d'une vidéo (via son Uri) respecte une limite donnée en secondes.
     */
    fun isVideoDurationValid(context: Context, uri: Uri, maxSeconds: Int): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            durationMs <= maxSeconds * 1000L
        } catch (e: Exception) {
            android.util.Log.e("VideoUtils", "Erreur lecture durée vidéo: ${e.message}")
            false
        } finally {
            retriever.release()
        }
    }
}
