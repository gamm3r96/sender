package com.example.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utility for providing tactile haptic feedback during QR stream scanning and frame assembly.
 */
object HapticFeedbackHelper {

    /**
     * Crisp, micro-haptic tick when a valid QR stream frame chunk is decoded & validated.
     */
    fun vibrateFrameDetected(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(25L, 160)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20L)
            }
        } catch (_: Exception) {
            // Silently swallow if device does not support vibration or is in restricted mode
        }
    }

    /**
     * Rich, dual-pulse confirmation vibration when 100% of stream chunks are assembled & verified.
     */
    fun vibrateStreamCompleted(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Distinctive heavy double-pulse pattern: [wait 0ms, buzz 70ms, pause 70ms, buzz 160ms]
                val timings = longArrayOf(0, 70, 70, 160)
                val amplitudes = intArrayOf(0, 220, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 70, 70, 160), -1)
            }
        } catch (_: Exception) {
            // Silently swallow
        }
    }

    /**
     * Subtle error/warning haptic buzz if corrupted chunk is intercepted.
     */
    fun vibrateCorruptedChunk(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 40, 50, 40)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50L)
            }
        } catch (_: Exception) {
            // Silently swallow
        }
    }

    /**
     * Warning pulse pattern when a stream times out without full completion.
     */
    fun vibrateTimeoutAlert(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 100, 100, 100, 180)
                val amplitudes = intArrayOf(0, 220, 0, 220, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 100, 100, 100, 180), -1)
            }
        } catch (_: Exception) {
            // Silently swallow
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
