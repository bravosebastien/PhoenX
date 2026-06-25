package com.example.phoenx.data.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticManager @Inject constructor(@ApplicationContext context: Context) {

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
            vibrateOreoPlus(longArrayOf(0, 100, 100, 100), intArrayOf(0, 150, 0, 150))
        } else {
            vibrateLegacy(longArrayOf(0, 100, 100, 100))
        }
    }

    /**
     * Une vibration longue et douce pour dire "C'est enregistré"
     */
    fun signalSaveSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateOreoPlusOneShot(500)
        } else {
            vibrateLegacyOneShot(500)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateOreoPlus(timings: LongArray, amplitudes: IntArray) {
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateOreoPlusOneShot(duration: Long) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(pattern: LongArray) {
        vibrator.vibrate(pattern, -1)
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacyOneShot(duration: Long) {
        vibrator.vibrate(duration)
    }
}
