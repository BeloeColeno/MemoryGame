package com.petrov.memory.domain.model

/**
 * Модель игровой комнаты для онлайн-режима
 */
data class OnlineGameRoom(
    val roomId: String = "",
    val hostPlayerId: String = "",
    val guestPlayerId: String? = null,
    val level: Int = 1,
    val timerMode: String = TimerMode.WITHOUT_TIMER.name,
    val timeLimit: Int? = null,
    val gameStarted: Boolean = false,
    val gameFinished: Boolean = false,
    val currentPlayerId: String = "",
    val cards: List<OnlineCard> = emptyList(),
    val hostScore: Int = 0,
    val guestScore: Int = 0,
    val hostPairs: Int = 0,
    val guestPairs: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "roomId" to roomId,
            "hostPlayerId" to hostPlayerId,
            "guestPlayerId" to guestPlayerId,
            "level" to level,
            "timerMode" to timerMode,
            "timeLimit" to timeLimit,
            "gameStarted" to gameStarted,
            "gameFinished" to gameFinished,
            "currentPlayerId" to currentPlayerId,
            "cards" to cards.map { it.toMap() },
            "hostScore" to hostScore,
            "guestScore" to guestScore,
            "hostPairs" to hostPairs,
            "guestPairs" to guestPairs,
            "timestamp" to timestamp
        )
    }
}

/**
 * Карточка в онлайн-игре
 */
data class OnlineCard(
    val id: Int = 0,
    val imageResId: Int = 0,
    val pairId: Int = 0,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false,
    val matchedBy: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "imageResId" to imageResId,
            "pairId" to pairId,
            "isFlipped" to isFlipped,
            "isMatched" to isMatched,
            "matchedBy" to matchedBy
        )
    }
}

/**
 * Ход игрока в онлайн-игре
 */
data class OnlineMove(
    val playerId: String = "",
    val cardId: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "playerId" to playerId,
            "cardId" to cardId,
            "timestamp" to timestamp
        )
    }
}
