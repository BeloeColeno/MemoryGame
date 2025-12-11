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
    private val availableWidth: Int,
    private val availableHeight: Int,
    private val gap: Int,
    private val columns: Int,  // Добавлен параметр колонок
    private val onCardClick: (Int) -> Unit
) : RecyclerView.Adapter<CardsAdapter.CardViewHolder>() {

    private var cachedCardSize: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        
        // Вычисляем размер карточки ТОЛЬКО ОДИН РАЗ
        if (cachedCardSize == 0) {
            cachedCardSize = calculateCardSize(itemCount, availableWidth, availableHeight, gap, columns)
            
            android.util.Log.d("CardsAdapter", "Columns: $columns, CardSize: ${cachedCardSize}px")
        }
        
        // Устанавливаем ФИКСИРОВАННЫЙ размер карточки (не растягивается)
        val layoutParams = RecyclerView.LayoutParams(cachedCardSize, cachedCardSize)
        layoutParams.width = cachedCardSize
        layoutParams.height = cachedCardSize
        binding.root.layoutParams = layoutParams
        
        return CardViewHolder(binding)
    }
    
    /**
     * Вычисляем размер карточки на основе известного количества колонок
     */
    private fun calculateCardSize(totalCards: Int, width: Int, height: Int, gap: Int, cols: Int): Int {
        android.util.Log.d("CardsAdapter", "calculateCardSize: totalCards=$totalCards, width=$width, height=$height, gap=$gap, cols=$cols")
        
        // Защита от некорректных данных
        if (totalCards <= 0 || width <= 0 || height <= 0 || cols <= 0) {
            android.util.Log.e("CardsAdapter", "Invalid input! Using fallback")
            return 100 // Минимальное значение по умолчанию
        }
        
        val rows = (totalCards + cols - 1) / cols // Округление вверх
        
        // Вычисляем размер карточки
        val totalGapWidth = (cols - 1) * gap
        val totalGapHeight = (rows - 1) * gap
        
        val cardWidth = (width - totalGapWidth) / cols
        val cardHeight = (height - totalGapHeight) / rows
        
        // Карточки квадратные - берем минимальный размер
        val cardSize = minOf(cardWidth, cardHeight)
        
        android.util.Log.d("CardsAdapter", "Calculated: ${cols}×${rows}, cardSize=$cardSize (width=$cardWidth, height=$cardHeight)")
        
        return maxOf(cardSize, 10) // Минимум 10px
    }

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
        cachedCardSize = 0 // Сбрасываем кеш для пересчета
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
            // Скрываем карточки-заглушки
            if (card.isPlaceholder) {
                binding.root.visibility = android.view.View.INVISIBLE
                return
            }
            
            binding.root.visibility = android.view.View.VISIBLE
            
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

            // Обработчик клика (только для реальных карточек)
            binding.cardView.setOnClickListener {
                if (!card.isRevealed && !card.isMatched && !card.isPlaceholder) {
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
