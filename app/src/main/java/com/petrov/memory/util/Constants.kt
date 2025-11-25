package com.petrov.memory.util

class Constants {
    // Уровни сложности из ТЗ раздел 4.1.1.4
    val LEVEL_EASY = 1      // 4x2 = 8 карточек (4 пары)
    val LEVEL_MEDIUM = 2    // 4x3 = 12 карточек (6 пар)
    val LEVEL_HARD = 3      // 4x4 = 16 карточек (8 пар)

    // Режимы игры из ТЗ раздел 4.2.1
    val MODE_SINGLE = "single"
    val MODE_COOP = "coop"

    // Требования производительности из ТЗ 4.2.2
    val MAX_REACTION_TIME_MS = 100L    // ≤0.1с
    val MAX_CHECK_TIME_MS = 200L       // ≤0.2с
    val TARGET_FPS = 60

    // База данных
    val DATABASE_NAME = "memory_game_db"
    val DATABASE_VERSION = 1

    // SharedPreferences
    val PREFS_NAME = "memory_game_prefs"
    val PREF_SOUND_ENABLED = "sound_enabled"
    val PREF_VIBRATION_ENABLED = "vibration_enabled"
}