package com.petrov.memory.domain.model

/**
 * Режимы таймера
 * Из ЛР №3: С таймером (лимит времени) / Без таймера (скрытый подсчет)
 */
enum class TimerMode {
    WITH_TIMER,     // С ограничением времени
    WITHOUT_TIMER   // Без ограничения (скрытый таймер для очков)
}
