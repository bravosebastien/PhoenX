package com.example.phoenx.data.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Deux vibrations courtes pour dire "Je t'écoute"
     */
    fun signalStartRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 100, 100)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            val pattern = longArrayOf(0, 100, 100, 100)
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Une vibration longue et douce pour dire "C'est enregistré"
     */
    fun signalSaveSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}
