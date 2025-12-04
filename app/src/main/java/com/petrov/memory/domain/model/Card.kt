package com.petrov.memory.domain.model

/**
 * Модель карточки Memory
 * Из ТЗ раздел 4.1.1.1 - Подсистема игровой логики
 */
data class Card(
    val id: Int,                    // Уникальный ID карточки
    val imageResId: Int,            // Ресурс изображения (R.drawable.card1)
    val pairId: Int,                // ID пары (одинаковый у парных карточек)
    var isRevealed: Boolean = false, // Открыта ли карточка
    var isMatched: Boolean = false   // Найдена ли пара
)
