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
import com.petrov.memory.data.preferences.StatsManager
import com.petrov.memory.util.SoundManager
import com.petrov.memory.util.VibrationManager
import java.util.*

/**
 * Экран кооперативной игры
 */
class CoopGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoopGameBinding
    private lateinit var adapter: CardsAdapter
    private lateinit var settingsManager: SettingsManager
    private lateinit var statsManager: StatsManager
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
        statsManager = StatsManager(this)
        soundManager = SoundManager(this)
        vibrationManager = VibrationManager(this)
        
        isSoundEnabled = settingsManager.isSoundEnabled
        soundManager.setEnabled(isSoundEnabled)
        vibrationManager.setEnabled(settingsManager.isVibrationEnabled)

        val pairsCount = intent.getIntExtra("pairs_count", 4)
        val timerModeName = intent.getStringExtra("timer_mode") ?: TimerMode.WITHOUT_TIMER.name
        val timerLimit = intent.getIntExtra("timer_limit", 0)
        
        val timerMode = TimerMode.valueOf(timerModeName)

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

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val rvMargin = (density * 8).toInt()

        val topReserved = (density * 64).toInt()
        val bottomReserved = (density * 8).toInt()
        val sideMargins = (density * 32).toInt()

        val cardPadding = (density * 2).toInt()
        val effectiveGap = cardPadding * 2

        val availableWidth = screenWidth - sideMargins - (rvMargin * 2)
        val availableHeight = screenHeight - topReserved - bottomReserved - (rvMargin * 2)
        
        android.util.Log.d("CoopGameActivity", "Screen: ${screenWidth}x${screenHeight}, Available: ${availableWidth}x${availableHeight}, topReserved=$topReserved")
        
        val optimalColumns = calculateOptimalColumns(cards.size, availableWidth, availableHeight, effectiveGap)

        cardsWithPlaceholders = addPlaceholdersForSymmetry(cards, optimalColumns).toMutableList()

        binding.rvCards.layoutManager = GridLayoutManager(this, optimalColumns)

        if (binding.rvCards.itemDecorationCount == 0) {
            binding.rvCards.addItemDecoration(CenteredGridDecoration(effectiveGap, optimalColumns, cardsWithPlaceholders.size))
        }

        binding.rvCards.isNestedScrollingEnabled = false
        binding.rvCards.overScrollMode = View.OVER_SCROLL_NEVER
        
        android.util.Log.d("CoopGameActivity", "Setting spanCount=$optimalColumns for ${cards.size} cards (${cardsWithPlaceholders.size} with placeholders)")

        adapter = CardsAdapter(cardsWithPlaceholders, availableWidth, availableHeight, effectiveGap, optimalColumns) { position ->
            val card = cardsWithPlaceholders[position]
            onCardClicked(card)
        }
        binding.rvCards.adapter = adapter

        binding.rvCards.post {
            centerGrid(optimalColumns, effectiveGap)
        }

        updateUI()
    }

    private fun generateCards(): MutableList<Card> {
        val cardsList = mutableListOf<Card>()

        val allCardResources = listOf(
            R.drawable.card1, R.drawable.card2, R.drawable.card3, R.drawable.card4,
            R.drawable.card5, R.drawable.card6, R.drawable.card7, R.drawable.card8,
            R.drawable.card9, R.drawable.card10, R.drawable.card11, R.drawable.card12,
            R.drawable.card13, R.drawable.card14
        )

        val selectedCards = allCardResources.shuffled().take(coopGameState.totalPairs)

        var cardId = 0
        for (pairId in 0 until coopGameState.totalPairs) {
            val imageRes = selectedCards[pairId]
            cardsList.add(Card(cardId++, imageRes, pairId))
            cardsList.add(Card(cardId++, imageRes, pairId))
        }

        for (i in cardsList.size - 1 downTo 1) {
            val j = (0..i).random()
            val temp = cardsList[i]
            cardsList[i] = cardsList[j]
            cardsList[j] = temp
        }
        
        return cardsList
    }

    /**
     * Центрирует сетку карточек на экране (копия из GameActivity)
     */
    private fun centerGrid(columns: Int, gap: Int) {
        val firstChild = binding.rvCards.getChildAt(0) ?: return
        val cardSize = firstChild.width
        
        if (cardSize == 0) return
        
        val rows = (cards.size + columns - 1) / columns
        
        val totalGridWidth = cardSize * columns + gap * (columns - 1)
        val totalGridHeight = cardSize * rows + gap * (rows - 1)
        
        val parentWidth = binding.rvCards.width
        val parentHeight = binding.rvCards.height
        
        val horizontalPadding = maxOf(0, (parentWidth - totalGridWidth) / 2)
        val verticalPadding = maxOf(0, (parentHeight - totalGridHeight) / 2)
        
        binding.rvCards.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        
        android.util.Log.d("CoopGameActivity", "Grid centered: hPadding=$horizontalPadding, vPadding=$verticalPadding")
    }

    /**
     * Вычисляет оптимальное количество колонок для сетки (копия из GameActivity)
     * Предпочитает симметричные варианты (где последняя строка заполнена или близка к центру)
     */
    private fun calculateOptimalColumns(totalCards: Int, width: Int, height: Int, gap: Int): Int {
        var bestColumns = 1
        var maxCardSize = 0
        
        for (cols in 1..totalCards) {
            val rows = (totalCards + cols - 1) / cols
            val totalGapWidth = (cols - 1) * gap
            val totalGapHeight = (rows - 1) * gap
            
            val cardWidth = (width - totalGapWidth) / cols
            val cardHeight = (height - totalGapHeight) / rows
            
            if (cardWidth <= 0 || cardHeight <= 0) continue
            
            val cardSize = minOf(cardWidth, cardHeight)

            val totalGridWidth = cardSize * cols + gap * (cols - 1)
            val totalGridHeight = cardSize * rows + gap * (rows - 1)
            
            // Если сетка не помещается - пропускаем этот вариант
            if (totalGridWidth > width || totalGridHeight > height) {
                continue
            }
            
            // Бонус за симметричность (полная последняя строка или близко к ней)
            val lastRowItems = totalCards % cols
            val symmetryBonus = if (lastRowItems == 0) {
                cardSize / 10  // +10% за полностью заполненную сетку
            } else if (lastRowItems >= cols / 2) {
                cardSize / 20  // +5% за >50% заполненную последнюю строку
            } else {
                0
            }
            
            val effectiveSize = cardSize + symmetryBonus
            
            if (effectiveSize > maxCardSize) {
                maxCardSize = effectiveSize
                bestColumns = cols
            }
        }
        
        return bestColumns
    }

    /**
     * Добавляет заглушки для симметричного размещения карточек (копия из GameActivity)
     */
    private fun addPlaceholdersForSymmetry(realCards: List<Card>, columns: Int): List<Card> {
        val lastRowItems = realCards.size % columns
        
        if (lastRowItems == 0) {
            // Полная сетка - не нужны заглушки
            return realCards
        }
        
        val emptySpaces = columns - lastRowItems
        val leftPlaceholders = emptySpaces / 2
        val rightPlaceholders = emptySpaces - leftPlaceholders
        
        // Вычисляем где вставить заглушки
        val totalRows = (realCards.size + columns - 1) / columns
        val firstRowStart = 0
        val lastRowStart = (totalRows - 1) * columns
        
        val result = mutableListOf<Card>()
        
        // Распределяем заглушки чередуя: первая строка - слева, последняя - справа
        for (i in 0 until totalRows * columns) {
            val row = i / columns
            val col = i % columns
            
            when {
                row == 0 && col < leftPlaceholders -> {
                    // Первая строка - заглушки слева
                    result.add(Card(-1, 0, isPlaceholder = true))
                }
                row == totalRows - 1 && col >= lastRowItems + leftPlaceholders -> {
                    // Последняя строка - заглушки справа
                    result.add(Card(-1, 0, isPlaceholder = true))
                }
                else -> {
                    // Реальная карточка
                    val cardIndex = result.count { !it.isPlaceholder }
                    if (cardIndex < realCards.size) {
                        result.add(realCards[cardIndex])
                    }
                }
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
        if (isChecking) {
            android.util.Log.w("CoopGameActivity", "Checking in progress, blocking click")
            return
        }

        if (card.isRevealed || card.isMatched || card.isPlaceholder) {
            android.util.Log.w("CoopGameActivity", "Card already revealed/matched/placeholder")
            return
        }

        if (firstRevealedCard != null && secondRevealedCard != null) {
            android.util.Log.w("CoopGameActivity", "Already 2 cards revealed, blocking click")
            return
        }

        vibrationManager.vibrate(VibrationManager.VibrationType.LIGHT)
        
        card.isRevealed = true
        adapter.notifyItemChanged(cardsWithPlaceholders.indexOf(card))
        
        android.util.Log.d("CoopGameActivity", "Card revealed: id=${card.id}, pairId=${card.pairId}")

        when {
            firstRevealedCard == null -> {
                firstRevealedCard = card
                android.util.Log.d("CoopGameActivity", "First card set")
            }
            secondRevealedCard == null -> {
                secondRevealedCard = card
                isChecking = true
                android.util.Log.d("CoopGameActivity", "Second card set, checking in 600ms")
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
        android.util.Log.d("CoopGameActivity", "Checking match: first.pairId=${first.pairId}, second.pairId=${second.pairId}, currentPlayer=${coopGameState.currentPlayerId}")

        if (first.pairId == second.pairId) {
            first.isMatched = true
            second.isMatched = true
            
            vibrationManager.vibrate(VibrationManager.VibrationType.SUCCESS)

            // Начисляем очки
            val score = coopGameState.calculatePairScore()
            currentPlayer.pairsFound++
            currentPlayer.totalScore += score
            
            // Обновляем состояние
            val nextPlayerId = if (coopGameState.currentPlayerId == 1) 2 else 1
            coopGameState = coopGameState.copy(
                matchedPairs = coopGameState.matchedPairs + 1,
                totalMoves = coopGameState.totalMoves + 1,
                currentPlayerId = nextPlayerId
            )
            
            android.util.Log.d("CoopGameActivity", "Match found! Next player: $nextPlayerId")
            
            adapter.notifyDataSetChanged()
            updateUI()

            firstRevealedCard = null
            secondRevealedCard = null
            isChecking = false

            if (coopGameState.matchedPairs == coopGameState.totalPairs) {
                handler.postDelayed({ showGameComplete() }, 500)
            }
            
        } else {
            // Не совпали - показываем 1.5 секунды, затем переворачиваем
            vibrationManager.vibrate(VibrationManager.VibrationType.ERROR)
            
            android.util.Log.d("CoopGameActivity", "No match, hiding cards in 1500ms")

            handler.postDelayed({
                first.isRevealed = false
                second.isRevealed = false

                // Обновляем состояние и МЕНЯЕМ игрока
                val nextPlayerId = if (coopGameState.currentPlayerId == 1) 2 else 1
                coopGameState = coopGameState.copy(
                    currentPlayerId = nextPlayerId,
                    totalMoves = coopGameState.totalMoves + 1
                )
                
                android.util.Log.d("CoopGameActivity", "Cards hidden, next player: $nextPlayerId")
                
                adapter.notifyDataSetChanged()
                updateUI()

                firstRevealedCard = null
                secondRevealedCard = null
                isChecking = false
            }, 1500)
        }
    }

    private fun updateUI() {
        // Обновляем информацию игроков
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

        val winner = when {
            coopGameState.player1.totalScore > coopGameState.player2.totalScore -> coopGameState.player1
            coopGameState.player2.totalScore > coopGameState.player1.totalScore -> coopGameState.player2
            else -> null // Ничья
        }
        
        coopGameState = coopGameState.copy(
            isGameFinished = true,
            winner = winner
        )
        
        // Сохраняем статистику (только для победителя)
        val levelId = when (coopGameState.totalPairs) {
            4 -> 1
            6 -> 2
            9 -> 3
            else -> 1
        }
        
        val timeSeconds = (coopGameState.elapsedTime / 1000).toInt()
        
        winner?.let {
            val stars = 3
            statsManager.saveGameResult(
                mode = StatsManager.MODE_COOP,
                levelId = levelId,
                won = true,
                time = timeSeconds,
                moves = coopGameState.totalMoves,
                stars = stars
            )
        }
        
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
