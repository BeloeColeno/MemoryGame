package com.petrov.memory.ui.game

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.R
import com.petrov.memory.databinding.ActivityGameBinding
import com.petrov.memory.domain.model.Card

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
    private var isSoundEnabled = true

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

        // Открываем карточку с анимацией
        card.isRevealed = true
        adapter.updateCardWithFlip(position)

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
                    
                    adapter.updateCards(cards) // Обновляем для эффекта прозрачности
                    
                    Toast.makeText(this, "Пара найдена!", Toast.LENGTH_SHORT).show()

                    // Проверяем, закончилась ли игра
                    if (matchedPairs == totalPairs) {
                        handler.postDelayed({
                            showLevelCompleteDialog()
                        }, 500)
                    }
                } else {
                    // Не совпало - закрываем карточки с анимацией
                    first.isRevealed = false
                    second.isRevealed = false
                    
                    val firstIndex = cards.indexOf(first)
                    val secondIndex = cards.indexOf(second)
                    adapter.updateCardWithFlip(firstIndex)
                    adapter.updateCardWithFlip(secondIndex)
                }
            }

            // Сбрасываем выбор
            firstRevealedCard = null
            secondRevealedCard = null
            isChecking = false
        }, 800) // Даем время посмотреть на карточки
    }

    /**
     * Обновление UI (счетчик ходов)
     */
    private fun updateUI() {
        binding.tvMoves.text = "Ходов: $moves"
    }

    /**
     * Показать диалог завершения уровня
     */
    private fun showLevelCompleteDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_level_complete)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        // Кнопка "В меню"
        dialog.findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        // Кнопка "Повторить"
        dialog.findViewById<ImageButton>(R.id.btnReplay).setOnClickListener {
            dialog.dismiss()
            restartGame()
        }

        // Кнопка "Следующий уровень" (пока заглушка)
        dialog.findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            Toast.makeText(this, "Следующий уровень пока недоступен", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    /**
     * Перезапуск игры
     */
    private fun restartGame() {
        moves = 0
        matchedPairs = 0
        firstRevealedCard = null
        secondRevealedCard = null
        isChecking = false
        cards = generateCards()
        adapter.updateCards(cards)
        updateUI()
    }

    /**
     * Настройка слушателей кнопок
     */
    private fun setupListeners() {
        // Кнопка "В меню"
        binding.btnMenu.setOnClickListener {
            finish()
        }

        // Кнопка "Звук"
        binding.btnSound.setOnClickListener {
            isSoundEnabled = !isSoundEnabled
            val iconRes = if (isSoundEnabled) {
                android.R.drawable.ic_lock_silent_mode_off
            } else {
                android.R.drawable.ic_lock_silent_mode
            }
            binding.btnSound.setImageResource(iconRes)
            Toast.makeText(
                this,
                if (isSoundEnabled) "Звук включен" else "Звук выключен",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
