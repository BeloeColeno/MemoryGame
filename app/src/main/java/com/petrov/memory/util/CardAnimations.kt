package com.petrov.memory.util

import android.view.View

/**
 * Утилиты для анимаций карточек
 * Упрощенные анимации без багов
 */
object CardAnimations {

    /**
     * Простая анимация переворота карточки
     * Просто меняет изображение с fade эффектом
     */
    fun flipCard(view: View, onHalfway: () -> Unit, duration: Long = 150) {
        // Сначала сбрасываем все анимации
        view.animate().cancel()
        view.clearAnimation()
        
        // Fade out
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                // Меняем изображение
                onHalfway()
                
                // Fade in
                view.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .start()
            }
            .start()
    }

    /**
     * Анимация исчезновения найденной пары
     */
    fun fadeOut(view: View, duration: Long = 300) {
        view.animate().cancel()
        view.animate()
            .alpha(0.3f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(duration)
            .start()
    }
}
