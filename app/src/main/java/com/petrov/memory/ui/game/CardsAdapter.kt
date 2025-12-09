package com.petrov.memory.ui.game

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.petrov.memory.R
import com.petrov.memory.databinding.ItemCardBinding
import com.petrov.memory.domain.model.Card

/**
 * Адаптер для отображения карточек в RecyclerView
 * Из ТЗ раздел 4.1.1.2 - Подсистема пользовательского интерфейса
 */
class CardsAdapter(
    private var cards: List<Card>,
    private val onCardClick: (Int) -> Unit
) : RecyclerView.Adapter<CardsAdapter.CardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        
        // Вычисляем оптимальную сетку для карточек
        val displayMetrics = parent.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // Резервируем место для UI элементов
        val topBottomReserved = (density * 140).toInt()
        val sideMargins = (density * 32).toInt()
        val cardGap = (density * 6).toInt() // Минимальный зазор между карточками
        
        val availableWidth = screenWidth - sideMargins
        val availableHeight = screenHeight - topBottomReserved
        
        // УМНЫЙ АЛГОРИТМ: находим оптимальное соотношение колонок/рядов
        val gridLayout = calculateOptimalGrid(itemCount, availableWidth, availableHeight, cardGap)
        
        // Обновляем количество колонок в GridLayoutManager
        val layoutManager = (parent as? androidx.recyclerview.widget.RecyclerView)?.layoutManager 
            as? androidx.recyclerview.widget.GridLayoutManager
        layoutManager?.spanCount = gridLayout.columns
        
        // Размер квадратной карточки (используем минимальный, чтобы все поместилось)
        val cardSize = gridLayout.cardSize
        
        // Устанавливаем размер ячейки
        binding.root.layoutParams = RecyclerView.LayoutParams(cardSize, cardSize)
        
        return CardViewHolder(binding)
    }
    
    /**
     * Вычисляет оптимальную сетку для размещения карточек
     * Алгоритм пробует разные варианты колонок/рядов и выбирает тот,
     * который дает максимальный размер карточки
     */
    private fun calculateOptimalGrid(totalCards: Int, width: Int, height: Int, gap: Int): GridLayout {
        var bestLayout = GridLayout(1, totalCards, 0)
        var maxCardSize = 0
        
        // Пробуем разные варианты от 1 до totalCards колонок
        for (cols in 1..totalCards) {
            val rows = (totalCards + cols - 1) / cols // Округление вверх
            
            // Вычисляем размер карточки для этого варианта
            val cardWidth = (width - (cols - 1) * gap) / cols
            val cardHeight = (height - (rows - 1) * gap) / rows
            
            // Карточки квадратные - берем минимальное значение
            val cardSize = minOf(cardWidth, cardHeight)
            
            // Выбираем вариант с максимальным размером карточки
            if (cardSize > maxCardSize) {
                maxCardSize = cardSize
                bestLayout = GridLayout(cols, rows, cardSize)
            }
        }
        
        return bestLayout
    }
    
    /**
     * Данные об оптимальной сетке
     */
    private data class GridLayout(
        val columns: Int,
        val rows: Int,
        val cardSize: Int
    )

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position], position)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bind(cards[position], position, payloads[0])
        }
    }

    override fun getItemCount(): Int = cards.size

    fun updateCards(newCards: List<Card>) {
        cards = newCards
        notifyDataSetChanged()
    }

    /**
     * Обновить одну карточку с плавной анимацией
     */
    fun updateCardWithFlip(position: Int) {
        notifyItemChanged(position, "flip")
    }

    inner class CardViewHolder(
        private val binding: ItemCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: Card, position: Int) {
            bind(card, position, null)
        }

        fun bind(card: Card, position: Int, payload: Any?) {
            // ВАЖНО: Сбрасываем все трансформации перед bind
            binding.cardView.alpha = 1f
            binding.cardView.scaleX = 1f
            binding.cardView.scaleY = 1f
            binding.cardView.rotationY = 0f
            
            val imageRes = if (card.isRevealed || card.isMatched) {
                card.imageResId
            } else {
                R.drawable.cover
            }

            // Если есть payload "flip" - делаем анимацию
            if (payload == "flip") {
                animateFlip {
                    binding.ivCard.setImageResource(imageRes)
                }
            } else {
                // Без анимации
                binding.ivCard.setImageResource(imageRes)
            }

            // Прозрачность для найденных пар
            if (card.isMatched) {
                animateMatch()
            }

            // Обработчик клика
            binding.cardView.setOnClickListener {
                if (!card.isRevealed && !card.isMatched) {
                    onCardClick(position)
                }
            }
        }

        /**
         * Безопасная анимация переворота через ObjectAnimator
         */
        private fun animateFlip(onMiddle: () -> Unit) {
            val flipOut = ObjectAnimator.ofFloat(binding.cardView, "rotationY", 0f, 90f).apply {
                duration = 150
            }
            val flipIn = ObjectAnimator.ofFloat(binding.cardView, "rotationY", -90f, 0f).apply {
                duration = 150
            }

            AnimatorSet().apply {
                play(flipOut).before(flipIn)
                flipOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onMiddle()
                    }
                })
                start()
            }
        }

        /**
         * Анимация для найденных пар
         */
        private fun animateMatch() {
            val scaleX = ObjectAnimator.ofFloat(binding.cardView, "scaleX", 1f, 0.95f)
            val scaleY = ObjectAnimator.ofFloat(binding.cardView, "scaleY", 1f, 0.95f)
            val alpha = ObjectAnimator.ofFloat(binding.cardView, "alpha", 1f, 0.5f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 300
                start()
            }
        }
    }
}
