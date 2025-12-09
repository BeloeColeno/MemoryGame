package com.petrov.memory.domain.model

import android.graphics.Color

/**
 * Игрок в кооперативном режиме
 * Из ЛР №4: Совместный режим - два игрока с чередованием ходов
 */
data class Player(
    val id: Int,                    // ID игрока (1 или 2)
    val name: String,               // Имя ("Игрок 1" / "Игрок 2")
    val color: Int,                 // Цвет игрока (синий/красный)
    var pairsFound: Int = 0,        // Количество найденных пар
    var totalScore: Int = 0         // Общее количество очков
) {
    companion object {
        // Цвета игроков: синий и красный (из требований)
        val PLAYER1_COLOR = Color.parseColor("#2196F3") // Синий
        val PLAYER2_COLOR = Color.parseColor("#F44336") // Красный
        
        fun createPlayer1() = Player(
            id = 1,
            name = "Игрок 1",
            color = PLAYER1_COLOR
        )
        
        fun createPlayer2() = Player(
            id = 2,
            name = "Игрок 2",
            color = PLAYER2_COLOR
        )
    }
}
