package com.petrov.memory.domain.model

/**
 * Общая статистика по всем уровням и режимам
 */
data class GameStats(
    val totalGamesPlayed: Int = 0,
    val totalGamesWon: Int = 0,
    val totalStars: Int = 0,
    val totalTime: Int = 0,
    val level1: LevelStats = LevelStats(1),
    val level2: LevelStats = LevelStats(2),
    val level3: LevelStats = LevelStats(3)
) {
    fun getLevelStats(levelId: Int): LevelStats {
        return when (levelId) {
            1 -> level1
            2 -> level2
            3 -> level3
            else -> LevelStats(levelId)
        }
    }
}

/**
 * Статистика по режиму игры
 */
data class ModeStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalTime: Int = 0,
    val totalStars: Int = 0,
    val level1: LevelStats = LevelStats(1),
    val level2: LevelStats = LevelStats(2),
    val level3: LevelStats = LevelStats(3)
) {
    fun getLevelStats(levelId: Int): LevelStats {
        return when (levelId) {
            1 -> level1
            2 -> level2
            3 -> level3
            else -> LevelStats(levelId)
        }
    }
}
