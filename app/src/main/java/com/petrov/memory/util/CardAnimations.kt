package com.petrov.memory.util

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

/**
 * Утилиты для анимаций карточек
 * Flip анимация для переворота карточек
 */
object CardAnimations {

    /**
     * Анимация переворота карточки с изменением изображения
     * @param view Карточка для анимации
     * @param onHalfway Callback на середине анимации (когда карточка повернута на 90°)
     * @param duration Длительность анимации в мс
     */
    fun flipCard(view: View, onHalfway: () -> Unit, duration: Long = 200) {
        // Первая половина: поворот от 0° до 90° (карточка исчезает)
        view.animate()
            .rotationY(90f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // В середине анимации меняем изображение
                onHalfway()
                
                // Вторая половина: поворот от -90° до 0° (карточка появляется)
                view.rotationY = -90f
                view.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /**
     * Анимация исчезновения найденной пары
     */
    fun fadeOut(view: View, duration: Long = 300) {
        view.animate()
            .alpha(0.3f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(duration)
            .start()
    }

    /**
     * Анимация "тряски" при неправильной паре
     */
    fun shake(view: View) {
        view.animate()
            .translationX(-10f)
            .setDuration(50)
            .withEndAction {
                view.animate()
                    .translationX(10f)
                    .setDuration(50)
                    .withEndAction {
                        view.animate()
                            .translationX(0f)
                            .setDuration(50)
                            .start()
                    }
                    .start()
            }
            .start()
    }
}
