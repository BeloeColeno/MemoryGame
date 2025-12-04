package com.petrov.memory.ui.game

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.petrov.memory.R
import com.petrov.memory.databinding.ItemCardBinding
import com.petrov.memory.domain.model.Card
import com.petrov.memory.util.CardAnimations

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
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position], position)
    }

    override fun getItemCount(): Int = cards.size

    fun updateCards(newCards: List<Card>) {
        cards = newCards
        notifyDataSetChanged()
    }

    /**
     * Обновить одну карточку с анимацией
     */
    fun updateCardWithAnimation(position: Int) {
        notifyItemChanged(position)
    }

    inner class CardViewHolder(
        private val binding: ItemCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: Card, position: Int) {
            // Отображаем изображение карточки
            val imageRes = if (card.isRevealed || card.isMatched) {
                card.imageResId
            } else {
                R.drawable.cover // Закрытая карточка
            }
            
            // Устанавливаем изображение без анимации при первой загрузке
            binding.ivCard.setImageResource(imageRes)

            // Прозрачность для найденных пар
            if (card.isMatched) {
                binding.cardView.alpha = 0.3f
                binding.cardView.scaleX = 0.95f
                binding.cardView.scaleY = 0.95f
            } else {
                binding.cardView.alpha = 1.0f
                binding.cardView.scaleX = 1.0f
                binding.cardView.scaleY = 1.0f
            }

            // Обработчик клика
            binding.cardView.setOnClickListener {
                if (!card.isRevealed && !card.isMatched) {
                    onCardClick(position)
                }
            }
        }
    }
}
