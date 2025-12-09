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
    private var totalPairs = 4 // По умолчанию легкий
    private var levelId = 1 // 1-легкий, 2-средний, 3-сложный
    private var gridColumns = 4 // Количество колонок в сетке
    private var startTime = 0L // Время старта игры

    private var firstRevealedCard: Card? = null
    private var secondRevealedCard: Card? = null
    private var isChecking = false
    private var isSoundEnabled = true

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val EXTRA_LEVEL_ID = "level_id"
        const val EXTRA_TOTAL_PAIRS = "total_pairs"
        const val EXTRA_GRID_COLUMNS = "grid_columns"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем параметры уровня из Intent
        levelId = intent.getIntExtra(EXTRA_LEVEL_ID, 1)
        totalPairs = intent.getIntExtra(EXTRA_TOTAL_PAIRS, 4)
        gridColumns = intent.getIntExtra(EXTRA_GRID_COLUMNS, 4)
        
        // Настраиваем сетку
        (binding.rvCards.layoutManager as? androidx.recyclerview.widget.GridLayoutManager)?.spanCount = gridColumns

        startTime = System.currentTimeMillis()
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
        
        // Добавляем декоратор для минимальных отступов между карточками (4dp)
        val spacing = (resources.displayMetrics.density * 4).toInt()
        binding.rvCards.addItemDecoration(CenteredGridDecoration(spacing))
        
        updateUI()
    }

    /**
     * Генерация карточек по алгоритму Fisher-Yates
     * Из ТЗ раздел 4.3.1 - Требования к математическому обеспечению
     */
    private fun generateCards(): MutableList<Card> {
        val cardsList = mutableListOf<Card>()
        
        // Все доступные карточки (14 штук)
        val allCardResources = listOf(
            R.drawable.card1, R.drawable.card2, R.drawable.card3, R.drawable.card4,
            R.drawable.card5, R.drawable.card6, R.drawable.card7, R.drawable.card8,
            R.drawable.card9, R.drawable.card10, R.drawable.card11, R.drawable.card12,
            R.drawable.card13, R.drawable.card14
        )
        
        // Выбираем случайные карточки для текущей игры
        val selectedCards = allCardResources.shuffled().take(totalPairs)

        // Создаем пары карточек
        var cardId = 0
        for (pairId in 0 until totalPairs) {
            val imageRes = selectedCards[pairId]
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
        val endTime = System.currentTimeMillis()
        val timeSeconds = ((endTime - startTime) / 1000).toInt()
        val stars = calculateStars(moves, timeSeconds)
        
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_level_complete)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        // Настраиваем звезды
        val star1 = dialog.findViewById<android.widget.ImageView>(R.id.ivStar1)
        val star2 = dialog.findViewById<android.widget.ImageView>(R.id.ivStar2)
        val star3 = dialog.findViewById<android.widget.ImageView>(R.id.ivStar3)
        
        // Затемняем звезды в зависимости от количества
        star1.alpha = if (stars >= 1) 1.0f else 0.3f
        star2.alpha = if (stars >= 2) 1.0f else 0.3f
        star3.alpha = if (stars >= 3) 1.0f else 0.3f

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

        // Кнопка "Следующий уровень"
        dialog.findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            if (levelId < 3) {
                dialog.dismiss()
                // Запускаем следующий уровень
                val nextLevel = levelId + 1
                val (pairs, cols) = when (nextLevel) {
                    2 -> Pair(6, 4) // Средний: 6 пар, 4 колонки
                    3 -> Pair(8, 4) // Сложный: 8 пар, 4 колонки
                    else -> Pair(4, 4)
                }
                intent.putExtra(EXTRA_LEVEL_ID, nextLevel)
                intent.putExtra(EXTRA_TOTAL_PAIRS, pairs)
                intent.putExtra(EXTRA_GRID_COLUMNS, cols)
                finish()
                startActivity(intent)
            } else {
                Toast.makeText(this, "Это последний уровень!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
        
        Toast.makeText(this, "Время: ${timeSeconds}с, Звезд: $stars", Toast.LENGTH_LONG).show()
    }
    
    /**
     * Расчет количества звезд (1-3) на основе ходов и времени
     */
    private fun calculateStars(moves: Int, timeSeconds: Int): Int {
        // Формула: меньше ходов и времени = больше звезд
        val perfectMoves = totalPairs * 2 // Идеальное количество ходов
        val perfectTime = totalPairs * 10 // Идеальное время (10 сек на пару)
        
        return when {
            moves <= perfectMoves && timeSeconds <= perfectTime -> 3
            moves <= perfectMoves * 1.5 && timeSeconds <= perfectTime * 1.5 -> 2
            else -> 1
        }
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
        startTime = System.currentTimeMillis()
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
