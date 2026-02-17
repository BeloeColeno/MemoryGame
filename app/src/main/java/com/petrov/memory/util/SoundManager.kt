package com.petrov.memory.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build

/**
 * Менеджер звуковых эффектов
 */
class SoundManager(private val context: Context) {
    
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var isEnabled = true
    
    enum class SoundType {
        CARD_FLIP,      // Переворот карточки
        MATCH_FOUND,    // Найдена пара
        NO_MATCH,       // Не совпали
        GAME_WIN,       // Победа
        BUTTON_CLICK    // Клик по кнопке
    }
    
    init {
        initSoundPool()
    }
    
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0)
        }
        
        // Загружаем звуки (будут добавлены позже)
        // soundMap[SoundType.CARD_FLIP] = soundPool?.load(context, R.raw.card_flip, 1) ?: 0
        // soundMap[SoundType.MATCH_FOUND] = soundPool?.load(context, R.raw.match_found, 1) ?: 0
        // soundMap[SoundType.NO_MATCH] = soundPool?.load(context, R.raw.no_match, 1) ?: 0
        // soundMap[SoundType.GAME_WIN] = soundPool?.load(context, R.raw.game_win, 1) ?: 0
        // soundMap[SoundType.BUTTON_CLICK] = soundPool?.load(context, R.raw.button_click, 1) ?: 0
    }
    
    /**
     * Воспроизвести звуковой эффект
     */
    fun playSound(soundType: SoundType) {
        if (!isEnabled) return
        
        val soundId = soundMap[soundType] ?: return
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }
    
    /**
     * Включить/выключить звуки
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Освободить ресурсы
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
    
    companion object {
        /**
         * Создать синтетический звук "клик"
         * Используется до добавления настоящих звуковых файлов
         */
        fun createClickSound(context: Context) {
            // TODO: Генерация простого звука или использование системного звука
        }
    }
}
