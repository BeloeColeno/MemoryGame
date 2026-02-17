package com.petrov.memory.domain.model

/**
 * Режимы таймера
 */
enum class TimerMode {
    WITH_TIMER,     // С ограничением времени
    WITHOUT_TIMER   // Без ограничения (скрытый таймер для очков)
}
