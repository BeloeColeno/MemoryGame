package com.petrov.memory.domain.model

/**
 * Режимы игры
 * Из ЛР №4: Проектирование интерфейса - форма "Выбор режима"
 */
enum class GameMode {
    SINGLE_PLAYER,   // Одиночная игра
    COOPERATIVE,     // Совместный режим (на одном экране)
    ONLINE           // Онлайн режим (два устройства через Firebase)
}
