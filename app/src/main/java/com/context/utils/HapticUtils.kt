package com.context.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtils {

    fun playTick(context: Context) {
        vibrate(context, VibrationEffect.EFFECT_TICK, 10L)
    }

    fun playClick(context: Context) {
        vibrate(context, VibrationEffect.EFFECT_CLICK, 20L)
    }

    fun playHeavyClick(context: Context) {
        vibrate(context, VibrationEffect.EFFECT_HEAVY_CLICK, 50L)
    }

    fun playDoubleTick(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(context, VibrationEffect.EFFECT_DOUBLE_CLICK, 20L)
        } else {
            vibratePattern(context, longArrayOf(0, 10, 50, 10), -1)
        }
    }

    fun playThud(context: Context) {
        // Long satisfying pulse for settlement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrate(context, -1, 100L, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            vibrateSimple(context, 100L)
        }
    }

    fun playWarning(context: Context) {
        // Three rapid pulses
        vibratePattern(context, longArrayOf(0, 30, 50, 30, 50, 30), -1)
    }

    private fun vibrate(context: Context, effectId: Int, fallbackMillis: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } catch (e: Exception) {
                vibrateSimple(context, fallbackMillis)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(fallbackMillis, amplitude))
        } else {
            vibrateSimple(context, fallbackMillis)
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray, repeat: Int) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } else {
            vibrator.vibrate(pattern, repeat)
        }
    }

    private fun vibrateSimple(context: Context, millis: Long) {
        getVibrator(context).vibrate(millis)
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
