package com.petrov.memory.domain.model

/**
 * Статистика по одному уровню
 */
data class LevelStats(
    val levelId: Int,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTime: Int = Int.MAX_VALUE,
    val bestMoves: Int = Int.MAX_VALUE,
    val bestStars: Int = 0,
    val totalStars: Int = 0
)
