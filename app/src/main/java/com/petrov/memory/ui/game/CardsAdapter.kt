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
        
        // Вычисляем размер карточки на основе размера экрана и количества колонок
        val displayMetrics = parent.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Учитываем отступы и количество колонок
        val layoutManager = (parent as? androidx.recyclerview.widget.RecyclerView)?.layoutManager 
            as? androidx.recyclerview.widget.GridLayoutManager
        val columns = layoutManager?.spanCount ?: 4
        val rows = when (columns) {
            4 -> if (itemCount <= 8) 2 else if (itemCount <= 12) 3 else 4
            else -> 2
        }
        
        // Размер карточки с учетом отступов и padding
        // 140dp для UI сверху/снизу, 32dp по бокам RecyclerView, 6dp padding самой карточки
        val topBottomReserved = (displayMetrics.density * 140).toInt()
        val sideMargins = (displayMetrics.density * 32).toInt()
        val cardPadding = (displayMetrics.density * 6).toInt() // 3dp с каждой стороны
        
        val availableHeight = screenHeight - topBottomReserved - (cardPadding * rows)
        val availableWidth = screenWidth - sideMargins - (cardPadding * columns)
        
        val cardHeight = availableHeight / rows
        val cardWidth = availableWidth / columns
        
        // Используем минимальный размер для квадратных карточек
        val cardSize = minOf(cardWidth, cardHeight)
        
        // Устанавливаем LayoutParams для RecyclerView
        binding.root.layoutParams = RecyclerView.LayoutParams(cardSize, cardSize)
        
        return CardViewHolder(binding)
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
