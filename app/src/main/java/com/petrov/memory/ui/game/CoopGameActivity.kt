package com.petrov.memory.ui.game

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.petrov.memory.R
import com.petrov.memory.databinding.ActivityCoopGameBinding
import com.petrov.memory.domain.model.*
import com.petrov.memory.data.preferences.SettingsManager
import com.petrov.memory.util.SoundManager
import com.petrov.memory.util.VibrationManager
import java.util.*

/**
 * Экран кооперативной игры
 * Из ЛР №3: Игровой процесс на двоих с чередованием ходов
 * Из ЛР №4: Подсчет очков, таймер, смена игроков
 */
class CoopGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoopGameBinding
    private lateinit var adapter: CardsAdapter
    private lateinit var settingsManager: SettingsManager
    private lateinit var soundManager: SoundManager
    private lateinit var vibrationManager: VibrationManager
    private lateinit var coopGameState: CoopGameState
    
    private var cards = mutableListOf<Card>()
    private var cardsWithPlaceholders = mutableListOf<Card>()
    private var firstRevealedCard: Card? = null
    private var secondRevealedCard: Card? = null
    private var isChecking = false
    private var isSoundEnabled = true
    
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoopGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        soundManager = SoundManager(this)
        vibrationManager = VibrationManager(this)
        
        isSoundEnabled = settingsManager.isSoundEnabled
        soundManager.setEnabled(isSoundEnabled)
        vibrationManager.setEnabled(settingsManager.isVibrationEnabled)

        // Получаем параметры игры
        val pairsCount = intent.getIntExtra("pairs_count", 4)
        val timerModeName = intent.getStringExtra("timer_mode") ?: TimerMode.WITHOUT_TIMER.name
        val timerLimit = intent.getIntExtra("timer_limit", 0)
        
        val timerMode = TimerMode.valueOf(timerModeName)

        // Инициализируем состояние игры
        coopGameState = CoopGameState(
            player1 = Player.createPlayer1(),
            player2 = Player.createPlayer2(),
            currentPlayerId = 1,
            timerMode = timerMode,
            timerLimit = timerLimit,
            totalPairs = pairsCount
        )

        setupGame()
        setupListeners()
        startTimer()
    }

    private fun setupGame() {
        cards = generateCards()
        
        // Вычисляем оптимальную сетку - RecyclerView сам обрабатывает margin из layout
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        // Учитываем только margin RecyclerView из layout (8dp * 2 = 16dp)
        val layoutMargin = (density * 16).toInt()
        
        // Резервируем место для верхней панели (~64dp высоты панели + 8dp margin сверху)
        val topReserved = (density * 72).toInt()
        
        // Зазор между карточками - увеличен для лучшей видимости
        val minGap = (density * 8).toInt()  // Увеличено с 4dp до 8dp
        
        // Доступное пространство = экран - отступы layout - резерв сверху
        val availableWidth = screenWidth - layoutMargin
        val availableHeight = screenHeight - topReserved - (density * 8).toInt() // 8dp снизу
        
        android.util.Log.d("CoopGameActivity", "Screen: ${screenWidth}x${screenHeight}, Available: ${availableWidth}x${availableHeight}, gap=$minGap")
        
        val optimalColumns = calculateOptimalColumns(cards.size, availableWidth, availableHeight, minGap)
        
        cardsWithPlaceholders = addPlaceholdersForSymmetry(cards, optimalColumns).toMutableList()
        
        binding.rvCards.layoutManager = GridLayoutManager(this, optimalColumns)
        
        // Отключаем скролл полностью
        binding.rvCards.isNestedScrollingEnabled = false
        binding.rvCards.overScrollMode = View.OVER_SCROLL_NEVER
        
        if (binding.rvCards.itemDecorationCount == 0) {
            binding.rvCards.addItemDecoration(
                CenteredGridDecoration(minGap, optimalColumns, cardsWithPlaceholders.size)
            )
        }

        adapter = CardsAdapter(cardsWithPlaceholders, availableWidth, availableHeight, minGap, optimalColumns) { position ->
            val card = cardsWithPlaceholders[position]
            onCardClicked(card)
        }
        binding.rvCards.adapter = adapter

        updateUI()
    }

    private fun generateCards(): MutableList<Card> {
        val cardsList = mutableListOf<Card>()
        
        // Все доступные карточки
        val allCardResources = listOf(
            R.drawable.card1, R.drawable.card2, R.drawable.card3, R.drawable.card4,
            R.drawable.card5, R.drawable.card6, R.drawable.card7, R.drawable.card8,
            R.drawable.card9, R.drawable.card10, R.drawable.card11, R.drawable.card12,
            R.drawable.card13, R.drawable.card14
        )
        
        // Выбираем случайные карточки для текущей игры
        val selectedCards = allCardResources.shuffled().take(coopGameState.totalPairs)

        // Создаем пары карточек
        var cardId = 0
        for (pairId in 0 until coopGameState.totalPairs) {
            val imageRes = selectedCards[pairId]
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

    private fun calculateOptimalColumns(totalCards: Int, width: Int, height: Int, gap: Int): Int {
        var bestCols = 1
        var maxCardSize = 0
        
        android.util.Log.d("CoopGameActivity", "calculateOptimalColumns: cards=$totalCards, w=$width, h=$height, gap=$gap")
        
        // Пробуем все возможные варианты сетки и выбираем тот, где карточки МАКСИМАЛЬНОГО размера
        for (cols in 1..totalCards) {
            val rows = (totalCards + cols - 1) / cols
            if (rows < 1) continue
            
            // Вычисляем размер карточки для этой конфигурации
            val cardWidth = (width - (gap * (cols - 1))) / cols
            val cardHeight = (height - (gap * (rows - 1))) / rows
            val cardSize = minOf(cardWidth, cardHeight)  // Карточки квадратные
            
            if (cardSize < 50) continue  // Слишком маленькие карточки
            
            // КРИТИЧНО: Проверяем, что вся сетка поместится в доступное пространство
            val totalGridWidth = cardSize * cols + gap * (cols - 1)
            val totalGridHeight = cardSize * rows + gap * (rows - 1)
            
            // Если сетка не помещается - пропускаем этот вариант
            if (totalGridWidth > width || totalGridHeight > height) {
                android.util.Log.d("CoopGameActivity", "  cols=$cols SKIPPED: grid ${totalGridWidth}x${totalGridHeight} > available ${width}x${height}")
                continue
            }
            
            android.util.Log.d("CoopGameActivity", "  cols=$cols, rows=$rows -> cardSize=$cardSize (grid: ${totalGridWidth}x${totalGridHeight})")
            
            // Выбираем вариант с МАКСИМАЛЬНЫМ размером карточки
            if (cardSize > maxCardSize) {
                maxCardSize = cardSize
                bestCols = cols
            }
        }
        
        android.util.Log.d("CoopGameActivity", "  BEST: cols=$bestCols, cardSize=$maxCardSize")
        
        return bestCols
    }

    private fun addPlaceholdersForSymmetry(cards: List<Card>, columns: Int): List<Card> {
        val totalCells = ((cards.size + columns - 1) / columns) * columns
        val placeholdersNeeded = totalCells - cards.size
        
        if (placeholdersNeeded == 0) return cards
        
        val result = mutableListOf<Card>()
        val rows = totalCells / columns
        val cardsPerRow = cards.size / rows
        val extraCards = cards.size % rows
        
        var cardIndex = 0
        for (row in 0 until rows) {
            val cardsInThisRow = cardsPerRow + if (row < extraCards) 1 else 0
            val placeholdersInRow = columns - cardsInThisRow
            
            val leftPlaceholders = placeholdersInRow / 2
            val rightPlaceholders = placeholdersInRow - leftPlaceholders
            
            repeat(leftPlaceholders) {
                result.add(Card(id = -1, imageResId = 0, isPlaceholder = true))
            }
            
            repeat(cardsInThisRow) {
                if (cardIndex < cards.size) {
                    result.add(cards[cardIndex++])
                }
            }
            
            repeat(rightPlaceholders) {
                result.add(Card(id = -1, imageResId = 0, isPlaceholder = true))
            }
        }
        
        return result
    }

    private fun setupListeners() {
        binding.btnMenu.setOnClickListener {
            finish()
        }
    }

    private fun onCardClicked(card: Card) {
        if (isChecking || card.isRevealed || card.isMatched || card.isPlaceholder) return

        vibrationManager.vibrate(VibrationManager.VibrationType.LIGHT)
        
        card.isRevealed = true
        adapter.notifyItemChanged(cardsWithPlaceholders.indexOf(card))

        when {
            firstRevealedCard == null -> {
                firstRevealedCard = card
            }
            secondRevealedCard == null -> {
                secondRevealedCard = card
                isChecking = true
                handler.postDelayed({ checkForMatch() }, 600)
            }
        }
    }

    private fun checkForMatch() {
        val first = firstRevealedCard
        val second = secondRevealedCard

        if (first == null || second == null) {
            isChecking = false
            return
        }

        val currentPlayer = coopGameState.getCurrentPlayer()

        if (first.pairId == second.pairId) {
            // Найдена пара!
            first.isMatched = true
            second.isMatched = true
            
            vibrationManager.vibrate(VibrationManager.VibrationType.SUCCESS)
            
            // Начисляем очки
            val score = coopGameState.calculatePairScore()
            currentPlayer.pairsFound++
            currentPlayer.totalScore += score
            
            // Обновляем состояние
            coopGameState = coopGameState.copy(
                matchedPairs = coopGameState.matchedPairs + 1,
                totalMoves = coopGameState.totalMoves + 1
            )
            
            // Если пара найдена, ход продолжается (не меняем игрока)
            
        } else {
            // Не совпали - переворачиваем обратно
            first.isRevealed = false
            second.isRevealed = false
            
            vibrationManager.vibrate(VibrationManager.VibrationType.ERROR)
            
            // Обновляем состояние и МЕНЯЕМ игрока
            coopGameState = coopGameState.copy(
                currentPlayerId = if (coopGameState.currentPlayerId == 1) 2 else 1,
                totalMoves = coopGameState.totalMoves + 1
            )
        }

        adapter.notifyDataSetChanged()
        updateUI()

        firstRevealedCard = null
        secondRevealedCard = null
        isChecking = false

        // Проверяем окончание игры
        if (coopGameState.matchedPairs == coopGameState.totalPairs) {
            handler.postDelayed({ showGameComplete() }, 500)
        }
    }

    private fun updateUI() {
        // Обновляем информацию игроков (новый компактный формат)
        binding.tvPlayer1Score.text = "${coopGameState.player1.totalScore} • ${coopGameState.player1.pairsFound} пар"
        binding.tvPlayer2Score.text = "${coopGameState.player2.totalScore} • ${coopGameState.player2.pairsFound} пар"
        
        // Обновляем индикатор хода
        val currentPlayer = coopGameState.getCurrentPlayer()
        binding.tvCurrentTurn.text = "Ход игрока ${currentPlayer.name.last()}"
        binding.tvCurrentTurn.setTextColor(currentPlayer.color)
        
        // Обновляем фоновый фильтр
        binding.viewPlayerBackground.setBackgroundColor(currentPlayer.color)
        
        // Показываем таймер если нужно
        if (coopGameState.timerMode == TimerMode.WITH_TIMER) {
            binding.tvTimer.visibility = View.VISIBLE
        }
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                coopGameState = coopGameState.copy(
                    elapsedTime = System.currentTimeMillis() - coopGameState.startTime
                )
                
                updateTimerDisplay()
                
                // Проверка истечения времени
                if (coopGameState.isTimeExpired()) {
                    showTimeExpired()
                    return
                }
                
                handler.postDelayed(this, 100)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun updateTimerDisplay() {
        if (coopGameState.timerMode == TimerMode.WITH_TIMER) {
            val remainingSec = coopGameState.timerLimit - (coopGameState.elapsedTime / 1000).toInt()
            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun showTimeExpired() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_coop_complete)
        dialog.setCancelable(false)

        dialog.findViewById<TextView>(R.id.tvTitle).text = "ВРЕМЯ ВЫШЛО!"
        dialog.findViewById<TextView>(R.id.tvResults).text = 
            "${coopGameState.player1.name}: ${coopGameState.player1.totalScore} очков\n" +
            "${coopGameState.player2.name}: ${coopGameState.player2.totalScore} очков"

        dialog.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            dialog.dismiss()
            recreate()
        }

        dialog.findViewById<Button>(R.id.btnMenu).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun showGameComplete() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        
        // Определяем победителя
        val winner = when {
            coopGameState.player1.totalScore > coopGameState.player2.totalScore -> coopGameState.player1
            coopGameState.player2.totalScore > coopGameState.player1.totalScore -> coopGameState.player2
            else -> null // Ничья
        }
        
        coopGameState = coopGameState.copy(
            isGameFinished = true,
            winner = winner
        )
        
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_coop_complete)
        dialog.setCancelable(false)

        val title = when {
            winner == null -> "НИЧЬЯ!"
            else -> "ПОБЕДИЛ ${winner.name}!"
        }
        
        dialog.findViewById<TextView>(R.id.tvTitle).text = title
        dialog.findViewById<TextView>(R.id.tvResults).text = 
            "Ходов: ${coopGameState.totalMoves}\n\n" +
            "${coopGameState.player1.name}: ${coopGameState.player1.totalScore} очков (${coopGameState.player1.pairsFound} пар)\n" +
            "${coopGameState.player2.name}: ${coopGameState.player2.totalScore} очков (${coopGameState.player2.pairsFound} пар)"

        dialog.findViewById<Button>(R.id.btnRestart).setOnClickListener {
            dialog.dismiss()
            recreate()
        }

        dialog.findViewById<Button>(R.id.btnMenu).setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
        
        // TODO: Сохранить результат в статистику
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { handler.removeCallbacks(it) }
        soundManager.release()
        vibrationManager.cancel()
    }
}
