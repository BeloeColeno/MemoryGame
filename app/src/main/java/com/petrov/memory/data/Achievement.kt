package com.petrov.memory.data

data class Achievement(
    val id: Int,
    val title: String,
    val description: String,
    val iconResId: Int,
    val isUnlocked: Boolean = false,
    val unlockedDate: Long = 0L
)

object AchievementManager {
    const val ACHIEVEMENT_FIRST_WIN = 1
    const val ACHIEVEMENT_SPEED_DEMON = 2
    const val ACHIEVEMENT_PERFECT_MEMORY = 3
    const val ACHIEVEMENT_PERSISTENT = 4
    const val ACHIEVEMENT_LEVEL_MASTER = 5
    const val ACHIEVEMENT_COOP_CHAMPION = 6
    const val ACHIEVEMENT_NIGHT_OWL = 7
    const val ACHIEVEMENT_COLLECTOR = 8
    const val ACHIEVEMENT_FLAWLESS = 9
    const val ACHIEVEMENT_MARATHON = 10

    fun getAllAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                id = ACHIEVEMENT_FIRST_WIN,
                title = "Первая победа",
                description = "Завершите свой первый уровень",
                iconResId = com.petrov.memory.R.drawable.achievement_first_win
            ),
            Achievement(
                id = ACHIEVEMENT_SPEED_DEMON,
                title = "Молниеносная память",
                description = "Завершите уровень менее чем за 30 секунд",
                iconResId = com.petrov.memory.R.drawable.achievement_speed
            ),
            Achievement(
                id = ACHIEVEMENT_PERFECT_MEMORY,
                title = "Идеальная память",
                description = "Завершите уровень без единой ошибки",
                iconResId = com.petrov.memory.R.drawable.achievement_perfect
            ),
            Achievement(
                id = ACHIEVEMENT_PERSISTENT,
                title = "Настойчивый",
                description = "Сыграйте 10 игр подряд",
                iconResId = com.petrov.memory.R.drawable.achievement_persistent
            ),
            Achievement(
                id = ACHIEVEMENT_LEVEL_MASTER,
                title = "Мастер уровней",
                description = "Пройдите все 5 уровней сложности",
                iconResId = com.petrov.memory.R.drawable.achievement_master
            ),
            Achievement(
                id = ACHIEVEMENT_COOP_CHAMPION,
                title = "Командный игрок",
                description = "Выиграйте 5 кооперативных игр",
                iconResId = com.petrov.memory.R.drawable.achievement_coop
            ),
            Achievement(
                id = ACHIEVEMENT_NIGHT_OWL,
                title = "Полуночник",
                description = "Сыграйте игру после полуночи",
                iconResId = com.petrov.memory.R.drawable.achievement_night
            ),
            Achievement(
                id = ACHIEVEMENT_COLLECTOR,
                title = "Коллекционер",
                description = "Откройте все 14 видов карточек",
                iconResId = com.petrov.memory.R.drawable.achievement_collector
            ),
            Achievement(
                id = ACHIEVEMENT_FLAWLESS,
                title = "Безупречный",
                description = "Пройдите 3 уровня подряд без ошибок",
                iconResId = com.petrov.memory.R.drawable.achievement_flawless
            ),
            Achievement(
                id = ACHIEVEMENT_MARATHON,
                title = "Марафонец",
                description = "Проведите в игре более 1 часа",
                iconResId = com.petrov.memory.R.drawable.achievement_marathon
            )
        )
    }
}
