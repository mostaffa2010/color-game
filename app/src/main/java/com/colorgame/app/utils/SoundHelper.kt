package com.colorgame.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View

class SoundHelper(private val context: Context) {

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun playClickHaptic(view: View) {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {
            vibrate(20)
        }
    }

    fun playSuccessHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrate(80)
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMs)
        } catch (_: Exception) {
        }
    }
}
