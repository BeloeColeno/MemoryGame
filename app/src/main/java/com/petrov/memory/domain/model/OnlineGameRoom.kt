package com.petrov.memory.domain.model

/**
 * Модель игровой комнаты для онлайн-режима
 * ВАЖНО: Все поля var для корректной десериализации Firebase!
 */
data class OnlineGameRoom(
    var roomId: String = "",
    var hostPlayerId: String = "",
    var guestPlayerId: String? = null,
    var level: Int = 1,
    var timerMode: String = TimerMode.WITHOUT_TIMER.name,
    var timeLimit: Int? = null,
    var gameStarted: Boolean = false,
    var gameFinished: Boolean = false,
    var currentPlayerId: String = "",
    var cards: List<OnlineCard> = emptyList(),
    var hostScore: Int = 0,
    var guestScore: Int = 0,
    var hostPairs: Int = 0,
    var guestPairs: Int = 0,
    var timestamp: Long = System.currentTimeMillis(),
    // Поля для отслеживания открытых карточек
    var firstFlippedCardId: Int? = null,
    var secondFlippedCardId: Int? = null,
    var checkingMatch: Boolean = false,
    var lastMovePlayerId: String? = null  // ID игрока, который сделал последний ход
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
            "timestamp" to timestamp,
            "firstFlippedCardId" to firstFlippedCardId,
            "secondFlippedCardId" to secondFlippedCardId,
            "checkingMatch" to checkingMatch,
            "lastMovePlayerId" to lastMovePlayerId
        )
    }
}

/**
 * Карточка в онлайн-игре
 * ВАЖНО: Все поля var для корректной десериализации Firebase!
 */
data class OnlineCard(
    var id: Int = 0,
    var imageResId: Int = 0,
    var pairId: Int = 0,
    var flipped: Boolean = false,
    var matched: Boolean = false,
    var matchedBy: String? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "imageResId" to imageResId,
            "pairId" to pairId,
            "flipped" to flipped,
            "matched" to matched,
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
