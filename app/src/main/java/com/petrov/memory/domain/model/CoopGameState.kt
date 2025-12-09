package com.petrov.memory.domain.model

/**
 * Состояние кооперативной игры
 * Из ЛР №3: Ключевое требование 4.1.1 - кооп на 2 игроков с соревнованием
 */
data class CoopGameState(
    val player1: Player,                    // Игрок 1
    val player2: Player,                    // Игрок 2
    val currentPlayerId: Int,               // ID текущего игрока (1 или 2)
    val timerMode: TimerMode,               // Режим таймера
    val timerLimit: Int,                    // Лимит времени (секунды, 0 если без лимита)
    val startTime: Long = System.currentTimeMillis(), // Время начала игры
    val elapsedTime: Long = 0L,             // Прошедшее время (миллисекунды)
    val totalMoves: Int = 0,                // Общее количество ходов
    val matchedPairs: Int = 0,              // Найденных пар
    val totalPairs: Int,                    // Всего пар
    val isGameFinished: Boolean = false,    // Игра завершена?
    val winner: Player? = null              // Победитель (null если игра не окончена)
) {
    /**
     * Получить текущего игрока
     */
    fun getCurrentPlayer(): Player {
        return if (currentPlayerId == 1) player1 else player2
    }
    
    /**
     * Получить противника
     */
    fun getOpponentPlayer(): Player {
        return if (currentPlayerId == 1) player2 else player1
    }
    
    /**
     * Проверка истечения времени (для режима с таймером)
     */
    fun isTimeExpired(): Boolean {
        if (timerMode != TimerMode.WITH_TIMER) return false
        return elapsedTime >= (timerLimit * 1000L)
    }
    
    /**
     * Вычислить очки за найденную пару
     * Формула из требований:
     * - С таймером: 100 × (Оставшееся / Лимит) × 2
     * - Без таймера: 100 × (1 - Затрачено / 300), минимум 10
     */
    fun calculatePairScore(): Int {
        val elapsedSec = (elapsedTime / 1000).toInt()
        
        return if (timerMode == TimerMode.WITH_TIMER) {
            val remaining = timerLimit - elapsedSec
            val score = (100.0 * remaining / timerLimit * 2).toInt()
            maxOf(score, 0)
        } else {
            val score = (100.0 * (1 - elapsedSec / 300.0)).toInt()
            maxOf(score, 10)
        }
    }
}
