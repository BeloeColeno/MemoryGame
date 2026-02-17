package com.petrov.memory.domain.model

/**
 * Модель карточки Memory
 */
data class Card(
    val id: Int,                    // Уникальный ID карточки
    val imageResId: Int,            // Ресурс изображения (R.drawable.card1)
    val pairId: Int = 0,            // ID пары (одинаковый у парных карточек)
    var isRevealed: Boolean = false, // Открыта ли карточка
    var isMatched: Boolean = false,  // Найдена ли пара
    val isPlaceholder: Boolean = false // Невидимая заглушка для симметрии
)
