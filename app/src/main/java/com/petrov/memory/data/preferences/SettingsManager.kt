package com.petrov.memory.data.preferences

import android.content.Context
import android.content.SharedPreferences

/**
 * Менеджер настроек приложения
 * Хранит пользовательские предпочтения
 */
class SettingsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "memory_game_settings"
        
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_THEME = "theme"
        
        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
    }
    
    /**
     * Звук включен/выключен
     */
    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
    
    /**
     * Вибрация включена/выключена
     */
    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
    
    /**
     * Тема приложения
     */
    var theme: Int
        get() = prefs.getInt(KEY_THEME, THEME_LIGHT)
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()
}
