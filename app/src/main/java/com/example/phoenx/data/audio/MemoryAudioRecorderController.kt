package com.example.phoenx.data.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsule l'enregistrement audio (note vocale) pour un souvenir.
 * Extrait de MemoryDetailViewModel — étape 3/7 du découpage.
 */
@Singleton
class MemoryAudioRecorderController @Inject constructor(
    private val wavRecorder: WavAudioRecorder,
    private val sttManager: SpeechToTextManager,
    @ApplicationContext private val context: Context
) {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isVoiceNoteOverlayOpen = MutableStateFlow(false)
    val isVoiceNoteOverlayOpen: StateFlow<Boolean> = _isVoiceNoteOverlayOpen.asStateFlow()

    val sttPartialText = sttManager.partialText

    private var currentAudioFile: File? = null

    fun openVoiceNoteOverlay() {
        _isVoiceNoteOverlayOpen.value = true
    }

    fun closeVoiceNoteOverlay() {
        _isVoiceNoteOverlayOpen.value = false
    }

    fun startAudioRecording() {
        val file = File(context.cacheDir, "temp_complement_${System.currentTimeMillis()}.wav")
        currentAudioFile = file

        android.util.Log.d("VoiceNoteDiag", "Démarrage WavRecorder (Alternative robuste)...")
        wavRecorder.start(file)
        _isRecording.value = true
    }

    /**
     * Arrête l'enregistrement et retourne le fichier audio final, ou null s'il n'y en a pas.
     */
    fun stopAudioRecording(): File? {
        android.util.Log.d("VoiceNoteDiag", "Arrêt de l'enregistrement demandé")
        _isRecording.value = false
        _isVoiceNoteOverlayOpen.value = false

        wavRecorder.stop()

        val file = currentAudioFile
        file?.let {
            val size = if (it.exists()) it.length() else -1
            android.util.Log.d("VoiceNoteDiag", "Fichier WAV final: ${it.absolutePath}, Taille: $size octets")
            inspectAudioFile(it)
        }
        return file
    }

    private fun inspectAudioFile(file: File) {
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(file.absolutePath)
            val trackCount = extractor.trackCount
            android.util.Log.d("VoiceNoteDiag", "DIAGNOSTIC MediaExtractor : $trackCount piste(s) trouvée(s)")
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                android.util.Log.d("VoiceNoteDiag", "Piste #$i : ${format.getString(MediaFormat.KEY_MIME)}")
            }
            extractor.release()
        } catch (e: Exception) {
            android.util.Log.e("VoiceNoteDiag", "ÉCHEC DIAGNOSTIC MediaExtractor : ${e.message}")
        }
    }
}