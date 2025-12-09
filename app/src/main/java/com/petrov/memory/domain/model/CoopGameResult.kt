package com.petrov.memory.domain.model

/**
 * Результат кооперативной игры для сохранения в статистику
 * Из ЛР №4: Требование отдельной статистики для кооперативного режима
 */
data class CoopGameResult(
    val timestamp: Long,            // Время игры
    val levelPairs: Int,            // Количество пар (4/6/9)
    val timerMode: TimerMode,       // Режим таймера
    val timerLimit: Int,            // Лимит времени (0 если без лимита)
    val totalTime: Long,            // Общее время игры (миллисекунды)
    val totalMoves: Int,            // Общее количество ходов
    val player1PairsFound: Int,     // Пар найдено игроком 1
    val player2PairsFound: Int,     // Пар найдено игроком 2
    val player1Score: Int,          // Очки игрока 1
    val player2Score: Int,          // Очки игрока 2
    val winnerId: Int               // ID победителя (1, 2, или 0 для ничьей)
) {
    companion object {
        fun fromCoopGameState(state: CoopGameState): CoopGameResult {
            val winnerId = when {
                state.winner == null -> 0
                state.winner.id == 1 -> 1
                else -> 2
            }
            
            return CoopGameResult(
                timestamp = System.currentTimeMillis(),
                levelPairs = state.totalPairs,
                timerMode = state.timerMode,
                timerLimit = state.timerLimit,
                totalTime = state.elapsedTime,
                totalMoves = state.totalMoves,
                player1PairsFound = state.player1.pairsFound,
                player2PairsFound = state.player2.pairsFound,
                player1Score = state.player1.totalScore,
                player2Score = state.player2.totalScore,
                winnerId = winnerId
            )
        }
    }
}
