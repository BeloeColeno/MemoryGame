package com.petrov.memory.domain.model

/**
 * Модель статистики игры
 */
data class GameStatistics(
    val levelId: Int,           // ID уровня (1-легкий, 2-средний, 3-сложный)
    val moves: Int,             // Количество ходов
    val timeSeconds: Int,       // Время прохождения в секундах
    val stars: Int,             // Количество звезд (1-3)
    val timestamp: Long = System.currentTimeMillis()  // Время прохождения
)
