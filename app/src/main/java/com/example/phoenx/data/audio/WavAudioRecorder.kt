package com.example.phoenx.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WavAudioRecorder — Enregistreur robuste PCM -> WAV.
 * Finalisation garantie de l'en-tête RIFF même en cas d'annulation.
 * v9.4.27
 */
@Singleton
class WavAudioRecorder @Inject constructor() {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingFile: File? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun start(outputFile: File) {
        if (isRecording) return
        
        recordingFile = outputFile
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("WavRecorder", "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch {
                writePcmToFile(outputFile)
            }
            Log.d("WavRecorder", "Enregistrement WAV démarré : ${outputFile.name}")
        } catch (e: Exception) {
            Log.e("WavRecorder", "Erreur au démarrage de l'enregistrement WAV", e)
        }
    }

    private suspend fun writePcmToFile(file: File) {
        try {
            FileOutputStream(file).use { fos ->
                val data = ByteArray(bufferSize)
                // Espace réservé pour l'en-tête (44 octets)
                repeat(44) { fos.write(0) }

                while (isRecording) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                    if (read > 0) {
                        fos.write(data, 0, read)
                    }
                }
                fos.flush()
            }
        } catch (e: Exception) {
            Log.e("WavRecorder", "Erreur pendant l'écriture PCM", e)
        } finally {
            // GARANTIE : On écrit l'en-tête même si la coroutine est annulée (v9.4.27)
            withContext(NonCancellable + Dispatchers.IO) {
                updateWavHeader(file)
            }
        }
    }

    fun stop() {
        if (!isRecording) return
        isRecording = false
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("WavRecorder", "Erreur à l'arrêt de AudioRecord", e)
        }
        audioRecord = null
        Log.d("WavRecorder", "Demande d'arrêt envoyée (en-tête en cours de finalisation...)")
    }

    private fun updateWavHeader(file: File) {
        if (!file.exists()) return
        val fileSize = file.length()
        val dataSize = fileSize - 44
        
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.writeBytes("RIFF") // Offset 0
            writeInt(raf, (36 + dataSize).toInt()) // Offset 4 : ChunkSize
            raf.writeBytes("WAVE") // Offset 8
            raf.writeBytes("fmt ") // Offset 12
            writeInt(raf, 16) // Offset 16 : Subchunk1Size
            writeShort(raf, 1.toShort()) // Offset 20 : AudioFormat (PCM = 1)
            writeShort(raf, 1.toShort()) // Offset 22 : NumChannels (Mono = 1)
            writeInt(raf, sampleRate) // Offset 24 : SampleRate
            writeInt(raf, sampleRate * 2) // Offset 28 : ByteRate (SampleRate * NumChannels * BitsPerSample/8)
            writeShort(raf, 2.toShort()) // Offset 32 : BlockAlign (NumChannels * BitsPerSample/8)
            writeShort(raf, 16.toShort()) // Offset 34 : BitsPerSample (16)
            raf.writeBytes("data") // Offset 36
            writeInt(raf, dataSize.toInt()) // Offset 40 : Subchunk2Size
        }
        Log.d("WavRecorder", "En-tête WAV finalisé avec succès. Taille données : $dataSize octets")
    }

    private fun writeInt(raf: RandomAccessFile, value: Int) {
        raf.write(value shr 0)
        raf.write(value shr 8)
        raf.write(value shr 16)
        raf.write(value shr 24)
    }

    private fun writeShort(raf: RandomAccessFile, value: Short) {
        raf.write(value.toInt() shr 0)
        raf.write(value.toInt() shr 8)
    }
}
