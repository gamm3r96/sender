package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utility for providing tactile haptic feedback during QR stream scanning, frame assembly,
 * transfer completions, cryptographic decryptions, and security operations.
 */
object HapticFeedbackHelper {

    private const val PREFS_NAME = "cipher_haptic_prefs"
    private const val KEY_HAPTIC_ENABLED = "haptic_feedback_enabled"

    fun isHapticEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    /**
     * Crisp, micro-haptic tick when a valid QR stream frame chunk is decoded & validated.
     */
    fun vibrateFrameDetected(context: Context) {
        if (!isHapticEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    vibrator.vibrate(effect)
                } catch (_: Exception) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20L, 120))
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(25L, 140)
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
        if (!isHapticEnabled(context)) return
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
            try {
                val vibrator = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Ascending triumphant tactile pulse on successful cryptographic decryption.
     */
    fun vibrateDecryptionSuccess(context: Context) {
        vibrateCelebrationSuccess(context)
    }

    /**
     * Distinct tactile pulse when a successful P2P connection is established.
     */
    fun vibrateConnectionSuccess(context: Context) {
        if (!isHapticEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Short, snappy double-click feel for connection establishment:
                // [wait 0ms, buzz 25ms, pause 40ms, buzz 35ms]
                val timings = longArrayOf(0, 25, 40, 35)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 25, 40, 35), -1)
            }
        } catch (_: Exception) {
            try {
                val vibrator = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Triumphant 4-stage crescendo celebratory haptic burst designed for Lottie-style success animations.
     */
    fun vibrateCelebrationSuccess(context: Context) {
        if (!isHapticEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 4-stage celebration crescendo pattern:
                // [wait 0ms, pop 35ms, pause 40ms, pop 50ms, pause 50ms, strong hit 90ms, pause 70ms, grand fanfare 160ms]
                val timings = longArrayOf(0, 35, 40, 50, 50, 90, 70, 160)
                val amplitudes = intArrayOf(0, 120, 0, 180, 0, 230, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 35, 40, 50, 50, 90, 70, 160), -1)
            }
        } catch (_: Exception) {
            vibrateStreamCompleted(context)
        }
    }

    /**
     * Distinctive deep pulse pattern when a P2P transfer finishes downloading successfully.
     */
    fun vibrateTransferSuccess(context: Context) {
        if (!isHapticEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Distinct pattern: [wait 0, buzz 60, pause 50, heavy buzz 100, pause 40, heavy buzz 100]
                val timings = longArrayOf(0, 60, 50, 100, 40, 100)
                val amplitudes = intArrayOf(0, 150, 0, 220, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 60, 50, 100, 40, 100), -1)
            }
        } catch (_: Exception) {
            vibrateCelebrationSuccess(context)
        }
    }

    /**
     * Staccato warning pattern on decryption error or incorrect passphrase.
     */
    fun vibratePassphraseError(context: Context) {
        if (!isHapticEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 3 sharp, urgent pulses: [wait 0ms, buzz 45ms, pause 45ms, buzz 45ms, pause 45ms, buzz 70ms]
                val timings = longArrayOf(0, 45, 45, 45, 45, 70)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 45, 45, 45, 45, 70), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Subtle error/warning haptic buzz if corrupted chunk is intercepted.
     */
    fun vibrateCorruptedChunk(context: Context) {
        if (!isHapticEnabled(context)) return
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
        } catch (_: Exception) {}
    }

    /**
     * Warning pulse pattern when a stream times out without full completion.
     */
    fun vibrateTimeoutAlert(context: Context) {
        if (!isHapticEnabled(context)) return
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
        } catch (_: Exception) {}
    }

    /**
     * Tactile feedback on biometric authentication success or passcode unlock.
     */
    fun vibrateBiometricSuccess(context: Context) {
        if (!isHapticEnabled(context)) return
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35L, 180))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Quick tactile click when a file is encrypted and animated QR stream is ready.
     */
    fun vibrateSendReady(context: Context) {
        vibrateBiometricSuccess(context)
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
