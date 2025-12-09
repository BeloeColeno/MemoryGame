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

    private var cardSize: Int = 0
    private var isLayoutCalculated = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        
        // Вычисляем размеры ТОЛЬКО ОДИН РАЗ при создании первого ViewHolder
        if (!isLayoutCalculated) {
            val displayMetrics = parent.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val density = displayMetrics.density
            
            // Резервируем место для UI элементов
            val topBottomReserved = (density * 140).toInt()
            val sideMargins = (density * 32).toInt()
            val minGap = (density * 4).toInt() // Минимальный зазор 4dp между карточками
            
            val availableWidth = screenWidth - sideMargins
            val availableHeight = screenHeight - topBottomReserved
            
            android.util.Log.d("CardsAdapter", "Screen: ${screenWidth}×${screenHeight}, Available: ${availableWidth}×${availableHeight}, Cards: $itemCount")
            
            // УМНЫЙ АЛГОРИТМ: находим оптимальный размер карточки
            val gridLayout = calculateOptimalCardSize(itemCount, availableWidth, availableHeight, minGap)
            
            android.util.Log.d("CardsAdapter", "Grid: ${gridLayout.columns}×${gridLayout.rows}, CardSize: ${gridLayout.cardSize}px")
            
            // Обновляем количество колонок в GridLayoutManager
            val layoutManager = (parent as? androidx.recyclerview.widget.RecyclerView)?.layoutManager 
                as? androidx.recyclerview.widget.GridLayoutManager
            layoutManager?.spanCount = gridLayout.columns
            
            cardSize = gridLayout.cardSize
            isLayoutCalculated = true
        }
        
        // Устанавливаем размер карточки
        binding.root.layoutParams = RecyclerView.LayoutParams(cardSize, cardSize)
        
        return CardViewHolder(binding)
    }
    
    /**
     * УЛУЧШЕННЫЙ АЛГОРИТМ размещения карточек
     * Находит максимальный размер квадратной карточки, которая поместится в игровую зону
     * с минимальными зазорами между карточками
     */
    private fun calculateOptimalCardSize(totalCards: Int, width: Int, height: Int, gap: Int): GridLayout {
        // Защита от некорректных данных
        if (totalCards <= 0 || width <= 0 || height <= 0) {
            return GridLayout(1, 1, 100) // Минимальные значения по умолчанию
        }
        
        var bestLayout = GridLayout(1, totalCards, 0)
        var maxCardSize = 0
        
        // Пробуем все возможные комбинации колонок и рядов
        for (cols in 1..totalCards) {
            val rows = (totalCards + cols - 1) / cols // Округление вверх
            
            // Вычисляем размер карточки для этой комбинации
            // Формула: (доступная_ширина - зазоры) / количество_колонок
            val totalGapWidth = (cols - 1) * gap
            val totalGapHeight = (rows - 1) * gap
            
            val cardWidth = (width - totalGapWidth) / cols
            val cardHeight = (height - totalGapHeight) / rows
            
            // Защита от отрицательных значений
            if (cardWidth <= 0 || cardHeight <= 0) continue
            
            // Карточки квадратные - берем минимальный размер
            val cardSize = minOf(cardWidth, cardHeight)
            
            // Проверяем, что карточки действительно помещаются
            val totalWidth = cardSize * cols + totalGapWidth
            val totalHeight = cardSize * rows + totalGapHeight
            
            if (totalWidth <= width && totalHeight <= height && cardSize > maxCardSize) {
                maxCardSize = cardSize
                bestLayout = GridLayout(cols, rows, cardSize)
            }
        }
        
        // Если не нашли подходящий вариант, используем минимальный размер
        if (maxCardSize == 0) {
            val minSize = minOf(width / totalCards, height / totalCards, 50)
            return GridLayout(totalCards, 1, maxOf(minSize, 10))
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
        isLayoutCalculated = false // Сбрасываем флаг при обновлении
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
