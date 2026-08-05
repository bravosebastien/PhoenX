package com.example.phoenx.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoenXAudioRecorder @Inject constructor(
    private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var startTime: Long = 0 // v9.4.27

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    fun start(outputFile: File) {
        startTime = System.currentTimeMillis() // v9.4.27
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            
            // v9.4.27 : Valeurs standards AAC pour garantir la validité du flux
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            
            // Utilisation du chemin direct (v9.4.27)
            setOutputFile(outputFile.absolutePath)

            prepare()
            start()

            recorder = this
        }
    }

    fun stop() {
        val duration = System.currentTimeMillis() - startTime
        Log.d("PhoenXAudio", "Arrêt de l'enregistrement après ${duration}ms") // v9.4.27

        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("PhoenXAudio", "MediaRecorder.stop() a levé une exception : ${e.message}")
        } finally {
            recorder?.reset()
            recorder?.release()
            recorder = null
        }
    }
}
