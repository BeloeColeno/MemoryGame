package com.petrov.memory.ui.game

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.R
import com.petrov.memory.databinding.ActivityGameBinding
import com.petrov.memory.domain.model.Card
import com.petrov.memory.util.CardAnimations

/**
 * Экран игры Memory
 * Из ТЗ раздел 4.1.1.1 - Подсистема игровой логики
 * Из ТЗ раздел 4.2.2 - Временной регламент (≤0.1с реакция, ≤0.2с проверка)
 */
class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var adapter: CardsAdapter
    private var cards = mutableListOf<Card>()
    private var moves = 0
    private var matchedPairs = 0
    private var totalPairs = 4 // Уровень ЛЕГКИЙ = 4 пары (из ТЗ 4.1.1.4)

    private var firstRevealedCard: Card? = null
    private var secondRevealedCard: Card? = null
    private var isChecking = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGame()
        setupListeners()
    }

    /**
     * Инициализация игры
     * Генерация карточек + Fisher-Yates shuffle (из ТЗ 4.3.1)
     */
    private fun setupGame() {
        cards = generateCards()
        adapter = CardsAdapter(cards) { position ->
            onCardClick(position)
        }
        binding.rvCards.adapter = adapter
        updateUI()
    }

    /**
     * Генерация карточек по алгоритму Fisher-Yates
     * Из ТЗ раздел 4.3.1 - Требования к математическому обеспечению
     */
    private fun generateCards(): MutableList<Card> {
        val cardsList = mutableListOf<Card>()
        val drawableResources = listOf(
            R.drawable.card1, R.drawable.card2, R.drawable.card3, R.drawable.card4
        )

        // Создаем пары карточек
        var cardId = 0
        for (pairId in 0 until totalPairs) {
            val imageRes = drawableResources[pairId]
            // Добавляем две одинаковые карточки (пара)
            cardsList.add(Card(cardId++, imageRes, pairId))
            cardsList.add(Card(cardId++, imageRes, pairId))
        }

        // Fisher-Yates shuffle
        for (i in cardsList.size - 1 downTo 1) {
            val j = (0..i).random()
            val temp = cardsList[i]
            cardsList[i] = cardsList[j]
            cardsList[j] = temp
        }

        return cardsList
    }

    /**
     * Обработка клика по карточке
     * Требование производительности: ≤0.1с (из ТЗ 4.2.2)
     */
    private fun onCardClick(position: Int) {
        if (isChecking) return

        val card = cards[position]
        if (card.isRevealed || card.isMatched) return

        // Получаем View карточки из RecyclerView
        val viewHolder = binding.rvCards.findViewHolderForAdapterPosition(position)
        val cardView = viewHolder?.itemView

        // Открываем карточку с анимацией
        card.isRevealed = true
        
        if (cardView != null) {
            CardAnimations.flipCard(cardView, onHalfway = {
                // В середине анимации обновляем изображение
                adapter.updateCardWithAnimation(position)
            })
        } else {
            adapter.updateCards(cards)
        }

        when {
            firstRevealedCard == null -> {
                // Первая карточка в ходе
                firstRevealedCard = card
            }
            secondRevealedCard == null -> {
                // Вторая карточка в ходе
                secondRevealedCard = card
                moves++
                updateUI()

                // Проверяем совпадение (≤0.2с из ТЗ 4.2.2)
                checkMatch()
            }
        }
    }

    /**
     * Проверка совпадения пары карточек
     * Требование производительности: ≤0.2с (из ТЗ 4.2.2)
     */
    private fun checkMatch() {
        isChecking = true

        handler.postDelayed({
            val first = firstRevealedCard
            val second = secondRevealedCard

            if (first != null && second != null) {
                if (first.pairId == second.pairId) {
                    // Совпадение!
                    first.isMatched = true
                    second.isMatched = true
                    matchedPairs++
                    
                    // Анимация исчезновения для найденных пар
                    val firstIndex = cards.indexOf(first)
                    val secondIndex = cards.indexOf(second)
                    
                    val firstViewHolder = binding.rvCards.findViewHolderForAdapterPosition(firstIndex)
                    val secondViewHolder = binding.rvCards.findViewHolderForAdapterPosition(secondIndex)
                    
                    firstViewHolder?.itemView?.let { CardAnimations.fadeOut(it) }
                    secondViewHolder?.itemView?.let { CardAnimations.fadeOut(it) }
                    
                    Toast.makeText(this, "Пара найдена!", Toast.LENGTH_SHORT).show()

                    // Проверяем, закончилась ли игра
                    if (matchedPairs == totalPairs) {
                        Toast.makeText(
                            this,
                            "Победа! Ходов: $moves",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    adapter.updateCards(cards)
                    
                    // Сбрасываем выбор сразу после совпадения
                    firstRevealedCard = null
                    secondRevealedCard = null
                    isChecking = false
                } else {
                    // Не совпало - закрываем карточки с анимацией
                    val firstIndex = cards.indexOf(first)
                    val secondIndex = cards.indexOf(second)
                    
                    // Переворачиваем обратно БЕЗ тряски (упрощенно)
                    handler.postDelayed({
                        first.isRevealed = false
                        second.isRevealed = false
                        
                        val firstViewHolder = binding.rvCards.findViewHolderForAdapterPosition(firstIndex)
                        val secondViewHolder = binding.rvCards.findViewHolderForAdapterPosition(secondIndex)
                        
                        firstViewHolder?.itemView?.let { view ->
                            CardAnimations.flipCard(view, onHalfway = {
                                adapter.updateCardWithAnimation(firstIndex)
                            })
                        }
                        secondViewHolder?.itemView?.let { view ->
                            CardAnimations.flipCard(view, onHalfway = {
                                adapter.updateCardWithAnimation(secondIndex)
                            })
                        }
                        
                        adapter.updateCards(cards)
                        
                        // Сбрасываем выбор после закрытия
                        firstRevealedCard = null
                        secondRevealedCard = null
                        isChecking = false
                    }, 500)
                }
            }
        }, 600) // 600 мс чтобы пользователь успел увидеть обе карточки
    }

    /**
     * Обновление UI (счетчик ходов)
     */
    private fun updateUI() {
        binding.tvMoves.text = "Ходов: $moves"
    }

    /**
     * Перезапуск игры
     */
    private fun setupListeners() {
        binding.btnRestart.setOnClickListener {
            moves = 0
            matchedPairs = 0
            firstRevealedCard = null
            secondRevealedCard = null
            isChecking = false
            cards = generateCards()
            adapter.updateCards(cards)
            updateUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
