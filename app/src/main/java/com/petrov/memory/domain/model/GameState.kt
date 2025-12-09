package com.petrov.memory.domain.model

/**
 * Состояние игры Memory (одиночный режим)
 * Из ТЗ раздел 4.1.4 - Режимы функционирования системы
 */
data class GameState(
    val cards: List<Card>,              // Список всех карточек
    val moves: Int = 0,                 // Количество ходов
    val matchedPairs: Int = 0,          // Найденных пар
    val totalPairs: Int,                // Всего пар
    val isGameFinished: Boolean = false // Игра завершена?
)
