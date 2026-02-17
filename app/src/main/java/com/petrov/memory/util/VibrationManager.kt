package com.petrov.memory.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Менеджер вибрации
 * Из ЛР №2: Нефункциональные требования - Настройки вибрации
 */
class VibrationManager(context: Context) {
    
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private var isEnabled = true
    
    enum class VibrationType {
        LIGHT,      // Легкая вибрация (клик)
        MEDIUM,     // Средняя вибрация (переворот карточки)
        SUCCESS,    // Успех (найдена пара)
        ERROR       // Ошибка (не совпали)
    }
    
    /**
     * Воспроизвести вибрацию
     */
    fun vibrate(type: VibrationType) {
        if (!isEnabled || vibrator?.hasVibrator() != true) return
        
        val duration = when (type) {
            VibrationType.LIGHT -> 20L
            VibrationType.MEDIUM -> 40L
            VibrationType.SUCCESS -> 100L
            VibrationType.ERROR -> 50L
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                VibrationType.SUCCESS -> {
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 50, 50, 50),
                        intArrayOf(0, 255, 0, 255),
                        -1
                    )
                }
                VibrationType.ERROR -> {
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                else -> {
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                }
            }
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }
    
    /**
     * Включить/выключить вибрацию
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Отменить вибрацию
     */
    fun cancel() {
        vibrator?.cancel()
    }
}
