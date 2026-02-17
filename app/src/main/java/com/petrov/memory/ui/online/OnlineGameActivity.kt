package com.petrov.memory.ui.online

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.petrov.memory.R
import com.petrov.memory.data.firebase.FirebaseGameManager
import com.petrov.memory.data.preferences.StatsManager
import com.petrov.memory.databinding.ActivityOnlineGameBinding
import com.petrov.memory.domain.model.Card
import com.petrov.memory.domain.model.OnlineCard
import com.petrov.memory.domain.model.OnlineGameRoom
import com.petrov.memory.ui.game.CardsAdapter
import com.petrov.memory.ui.game.CenteredGridDecoration
import com.petrov.memory.util.SoundManager
import com.petrov.memory.util.VibrationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Экран онлайн-игры через Firebase
 */
class OnlineGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlineGameBinding
    private val firebaseManager = FirebaseGameManager.getInstance()
    private lateinit var statsManager: StatsManager
    
    private lateinit var roomId: String
    private var isHost: Boolean = false
    private var level: Int = 1
    private var myPlayerId: String = ""
    
    private var cardsAdapter: CardsAdapter? = null
    private val cards = mutableListOf<Card>()

    private var adapterParams: AdapterParams? = null
    
    private lateinit var soundManager: SoundManager
    private lateinit var vibrationManager: VibrationManager

    private var currentDialog: AlertDialog? = null

    private var isCheckingMatch = false

    private var resultDialogShown = false

    private var firstCardThisTurn: Int? = null
    private var secondCardThisTurn: Int? = null

    private val pendingClicks = mutableSetOf<Int>()

    private val handler = Handler(Looper.getMainLooper())
    private var gameStartTime: Long = 0L
    private var turnStartTime: Long = 0L
    private var timerRunnable: Runnable? = null
    private var turnTimerRunnable: Runnable? = null
    private val TURN_TIME_LIMIT = 10

    private var previousCurrentPlayerId: String? = null

    private data class AdapterParams(
        val availableWidth: Int,
        val availableHeight: Int,
        val gap: Int,
        val columns: Int,
        val totalCards: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: run {
            finish()
            return
        }
        
        isHost = intent.getBooleanExtra(EXTRA_IS_HOST, false)
        level = intent.getIntExtra(EXTRA_LEVEL, 1)

        soundManager = SoundManager(this)
        vibrationManager = VibrationManager(this)
        statsManager = StatsManager(this)

        binding.btnExit.setOnClickListener {
            showExitConfirmDialog()
        }

        lifecycleScope.launch {
            try {
                myPlayerId = firebaseManager.getCurrentPlayerId()
                if (myPlayerId.isEmpty()) {
                    Toast.makeText(this@OnlineGameActivity, "Ошибка аутентификации", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }
                setupGame()
                observeRoom()
            } catch (e: Exception) {
                android.util.Log.e("OnlineGameActivity", "Error initializing game", e)
                Toast.makeText(this@OnlineGameActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startGameTimer(room: OnlineGameRoom) {
        if (room.timerMode != "WITHOUT_TIMER" && room.timeLimit != null) {
            binding.tvTimer.visibility = android.view.View.VISIBLE
            gameStartTime = System.currentTimeMillis()
            
            timerRunnable = object : Runnable {
                override fun run() {
                    val elapsed = (System.currentTimeMillis() - gameStartTime) / 1000
                    val remaining = room.timeLimit!! - elapsed
                    
                    if (remaining > 0) {
                        val minutes = remaining / 60
                        val seconds = remaining % 60
                        binding.tvTimer.text = "⏱ ${String.format("%02d:%02d", minutes, seconds)}"
                        handler.postDelayed(this, 1000)
                    } else {
                        binding.tvTimer.text = "⏱ 00:00"
                        // Время вышло - завершаем игру с текущим счетом
                        lifecycleScope.launch {
                            try {
                                com.google.firebase.database.FirebaseDatabase.getInstance()
                                    .getReference("rooms/$roomId/gameFinished")
                                    .setValue(true)
                            } catch (e: Exception) {
                                android.util.Log.e("OnlineGameActivity", "Error ending game: ${e.message}")
                            }
                        }
                    }
                }
            }
            handler.post(timerRunnable!!)
        } else {
            binding.tvTimer.visibility = android.view.View.GONE
        }
    }
    
    private fun startTurnTimer() {
        turnTimerRunnable?.let { handler.removeCallbacks(it) }
        
        turnStartTime = System.currentTimeMillis()
        
        turnTimerRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - turnStartTime) / 1000
                val remaining = TURN_TIME_LIMIT - elapsed
                
                if (remaining > 0) {
                    binding.tvTurnTimer.text = "⏰ $remaining"
                    binding.tvTurnTimer.setTextColor(
                        if (remaining <= 3) 0xFFF44336.toInt() else 0xFFFFFFFF.toInt()
                    )
                    handler.postDelayed(this, 1000)
                } else {
                    binding.tvTurnTimer.text = "⏰ 0"
                    binding.tvTurnTimer.setTextColor(0xFFF44336.toInt())
                    // Время хода вышло - переключаем ход через Firebase напрямую
                    lifecycleScope.launch {
                        try {
                            val room = firebaseManager.observeRoom(roomId).first()
                            if (room != null && !room.gameFinished) {
                                val newPlayerId = if (room.currentPlayerId == room.hostPlayerId) {
                                    room.guestPlayerId ?: room.hostPlayerId
                                } else {
                                    room.hostPlayerId
                                }
                                
                                // Сбрасываем состояние хода
                                val updates = hashMapOf<String, Any>(
                                    "currentPlayerId" to newPlayerId,
                                    "firstFlippedCardId" to "",
                                    "secondFlippedCardId" to "",
                                    "checkingMatch" to false
                                )
                                
                                com.google.firebase.database.FirebaseDatabase.getInstance()
                                    .getReference("rooms/$roomId")
                                    .updateChildren(updates)
                                    
                                android.util.Log.d("OnlineGameActivity", "Turn timer expired, switched to $newPlayerId")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("OnlineGameActivity", "Error switching turn: ${e.message}")
                        }
                    }
                }
            }
        }
        handler.post(turnTimerRunnable!!)
    }
    
    private fun stopTurnTimer() {
        turnTimerRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun showExitConfirmDialog() {
        currentDialog?.dismiss()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_confirm, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            currentDialog?.dismiss()
            currentDialog = null
        }
        
        dialogView.findViewById<android.widget.Button>(R.id.btnExit).setOnClickListener {
            lifecycleScope.launch {
                firebaseManager.leaveRoom(roomId)
            }
            currentDialog?.dismiss()
            currentDialog = null
            finish()
        }
        
        currentDialog = dialog
        dialog.show()
    }

    private fun setupGame() {
        val (columns, rows) = getGridSize(level)
        val totalCards = columns * rows
        
        // Вычисляем доступное пространство для карточек
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val rvPadding = (density * 8).toInt()

        val topReserved = (density * 96).toInt()
        val bottomReserved = (density * 8).toInt()
        val sideMargins = (density * 32).toInt()

        val cardPadding = (density * 2).toInt()
        val effectiveGap = cardPadding * 2

        val availableWidth = screenWidth - sideMargins - (rvPadding * 2)
        val availableHeight = screenHeight - topReserved - bottomReserved - (rvPadding * 2)
        
        android.util.Log.d("OnlineGameActivity", "setupGame: Screen=${screenWidth}x${screenHeight}, Available=${availableWidth}x${availableHeight}, topReserved=$topReserved, level=$level, columns=$columns, totalCards=$totalCards")
        
        // Сохраняем параметры для последующего создания адаптера
        adapterParams = AdapterParams(
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            gap = effectiveGap,
            columns = columns,
            totalCards = totalCards
        )
        
        // Настраиваем RecyclerView
        binding.recyclerCards.apply {
            layoutManager = GridLayoutManager(this@OnlineGameActivity, columns)
            
            // Добавляем декоратор для центрирования сетки
            if (itemDecorationCount == 0) {
                addItemDecoration(CenteredGridDecoration(effectiveGap, columns, totalCards))
            }

            isNestedScrollingEnabled = false
            overScrollMode = android.view.View.OVER_SCROLL_NEVER
        }
        
        android.util.Log.d("OnlineGameActivity", "setupGame: AdapterParams saved, waiting for cards from Firebase")
    }

    private fun getGridSize(level: Int): Pair<Int, Int> {
        return when (level) {
            1 -> Pair(4, 2) // 4 колонки, 2 ряда (4 пары)
            2 -> Pair(4, 3) // 4 колонки, 3 ряда (6 пар)
            3 -> Pair(6, 3) // 6 колонок, 3 ряда (9 пар)
            else -> Pair(4, 2)
        }
    }

    private fun observeRoom() {
        lifecycleScope.launch {
            firebaseManager.observeRoom(roomId).collect { room ->
                if (room == null) {
                    showGameEndedDialog("Комната закрыта. Соперник покинул игру.")
                    return@collect
                }
                
                // Проверка на отключение соперника
                if (room.gameStarted && !room.gameFinished) {
                    val opponentDisconnected = if (myPlayerId == room.hostPlayerId) {
                        room.guestPlayerId == null
                    } else {
                        false
                    }
                    
                    if (opponentDisconnected) {
                        showGameEndedDialog("Соперник отключился. Вы победили!")
                        lifecycleScope.launch {
                            firebaseManager.leaveRoom(roomId)
                        }
                        return@collect
                    }
                }
                
                updateUI(room)
                
                if (room.gameFinished) {
                    if (!resultDialogShown) {
                        resultDialogShown = true
                        showResultDialog(room)
                    }
                }
            }
        }
    }

    private fun updateUI(room: OnlineGameRoom) {
        android.util.Log.d("OnlineGameActivity", "updateUI: Got ${room.cards.size} cards from Firebase, adapter=${cardsAdapter != null}")
        
        // Логируем первые 3 карточки для отладки
        room.cards.take(3).forEachIndexed { index, card ->
            android.util.Log.d("OnlineGameActivity", "Card[$index]: id=${card.id}, pairId=${card.pairId}, flipped=${card.flipped}, matched=${card.matched}")
        }
        
        // ВСЕГДА пересоздаем список карточек из Firebase
        val newCards = room.cards.map { onlineCard ->
            Card(
                id = onlineCard.id,
                imageResId = getImageResource(onlineCard.pairId),  // Используем pairId!
                isRevealed = onlineCard.flipped,
                isMatched = onlineCard.matched
            )
        }
        
        android.util.Log.d("OnlineGameActivity", "updateUI: Created ${newCards.size} Card objects")
        newCards.take(3).forEachIndexed { index, card ->
            android.util.Log.d("OnlineGameActivity", "LocalCard[$index]: id=${card.id}, isRevealed=${card.isRevealed}, isMatched=${card.isMatched}, imageRes=${card.imageResId}")
        }
        
        // Обновляем список карточек
        cards.clear()
        cards.addAll(newCards)
        
        // Создаем адаптер при первом получении карточек
        if (cardsAdapter == null && newCards.isNotEmpty()) {
            val params = adapterParams
            if (params != null) {
                android.util.Log.d("OnlineGameActivity", "updateUI: Creating adapter with ${newCards.size} cards, columns=${params.columns}")
                
                runOnUiThread {
                    cardsAdapter = CardsAdapter(
                        cards = newCards,
                        availableWidth = params.availableWidth,
                        availableHeight = params.availableHeight,
                        gap = params.gap,
                        columns = params.columns
                    ) { position ->
                        val card = cards.getOrNull(position)
                        if (card != null) {
                            onCardClick(card)
                        }
                    }
                    
                    binding.recyclerCards.adapter = cardsAdapter
                    android.util.Log.d("OnlineGameActivity", "updateUI: Adapter created and set to RecyclerView")
                }
            } else {
                android.util.Log.e("OnlineGameActivity", "updateUI: AdapterParams is null!")
            }
        } else if (cardsAdapter != null) {
            runOnUiThread {
                cardsAdapter?.updateCardsWithAnimation(cards.toList())
                android.util.Log.d("OnlineGameActivity", "updateUI: Adapter updated with ${cards.size} cards")
            }
        }
        
        // Обновляем счет и проверяем смену хода
        val turnChanged = room.currentPlayerId != previousCurrentPlayerId
        
        runOnUiThread {
            binding.tvHostScore.text = "Игрок 1: ${room.hostPairs} пар"
            binding.tvGuestScore.text = "Игрок 2: ${room.guestPairs} пар"

            val isMyTurn = room.currentPlayerId == myPlayerId
            
            if (isMyTurn) {
                binding.tvYourTurn.setTextColor(getColor(R.color.player_turn_active))
                binding.tvOpponentTurn.setTextColor(0x80FFFFFF.toInt())

                if (turnChanged) {
                    startTurnTimer()
                }
            } else {
                binding.tvYourTurn.setTextColor(0x80FFFFFF.toInt())
                binding.tvOpponentTurn.setTextColor(getColor(R.color.player_opponent_turn))

                stopTurnTimer()
            }
            
            // Сохраняем текущего игрока для следующей проверки
            previousCurrentPlayerId = room.currentPlayerId
            
            // Запускаем таймер матча при первом обновлении
            if (gameStartTime == 0L && room.gameStarted) {
                startGameTimer(room)
            }
        }
        
        // АВТОМАТИЧЕСКАЯ ПРОВЕРКА СОВПАДЕНИЙ
        android.util.Log.d("OnlineGameActivity", "updateUI: checkingMatch=${room.checkingMatch}, " +
            "first=${room.firstFlippedCardId}, second=${room.secondFlippedCardId}, " +
            "currentPlayer=${room.currentPlayerId}, lastMovePlayer=${room.lastMovePlayerId}, myId=$myPlayerId, isCheckingMatch=$isCheckingMatch")
        
        if (room.checkingMatch && 
            room.firstFlippedCardId != null && 
            room.secondFlippedCardId != null &&
            room.lastMovePlayerId == myPlayerId &&
            !isCheckingMatch) {
            
            android.util.Log.d("OnlineGameActivity", "updateUI: Starting match check (I made last move)")
            isCheckingMatch = true
            
            // Запускаем проверку с задержкой для просмотра карточек
            Handler(Looper.getMainLooper()).postDelayed({
                lifecycleScope.launch {
                    try {
                        android.util.Log.d("OnlineGameActivity", "updateUI: Calling checkMatch now...")
                        firebaseManager.checkMatch(
                            roomId, 
                            room.firstFlippedCardId!!, 
                            room.secondFlippedCardId!!
                        )
                        android.util.Log.d("OnlineGameActivity", "updateUI: checkMatch completed")
                    } catch (e: Exception) {
                        android.util.Log.e("OnlineGameActivity", "Error checking match", e)
                    } finally {
                        isCheckingMatch = false
                        android.util.Log.d("OnlineGameActivity", "updateUI: isCheckingMatch reset to false")
                    }
                }
            }, 1000)
        }
        
        // Сбрасываем флаги, если проверка завершена
        if (!room.checkingMatch) {
            synchronized(this) {
                isCheckingMatch = false
                pendingClicks.clear()
                firstCardThisTurn = null
                secondCardThisTurn = null
            }
            android.util.Log.d("OnlineGameActivity", "updateUI: isCheckingMatch, pendingClicks and turn cards reset")
        }
        
        // Также сбрасываем карты хода при смене игрока
        if (turnChanged) {
            synchronized(this) {
                firstCardThisTurn = null
                secondCardThisTurn = null
            }
            android.util.Log.d("OnlineGameActivity", "updateUI: Turn changed, turn cards reset")
        }
    }

    private fun onCardClick(card: Card) {
        if (card.isMatched || card.isRevealed) {
            android.util.Log.d("OnlineGameActivity", "onCardClick: Card already revealed or matched")
            return
        }
        
        // Проверяем что не идет проверка совпадения
        if (isCheckingMatch) {
            android.util.Log.d("OnlineGameActivity", "onCardClick: Match check in progress, ignoring click")
            return
        }
        
        // Проверяем что этот клик еще не обрабатывается
        if (pendingClicks.contains(card.id)) {
            android.util.Log.d("OnlineGameActivity", "onCardClick: Card ${card.id} click already pending, ignoring")
            return
        }
        
        // АТОМАРНАЯ проверка и заполнение слота
        val slotAssigned = synchronized(this) {
            when {
                firstCardThisTurn == null -> {
                    firstCardThisTurn = card.id
                    pendingClicks.add(card.id)
                    android.util.Log.d("OnlineGameActivity", "onCardClick: Card ${card.id} is FIRST in turn")
                    true
                }
                secondCardThisTurn == null -> {
                    secondCardThisTurn = card.id
                    pendingClicks.add(card.id)
                    android.util.Log.d("OnlineGameActivity", "onCardClick: Card ${card.id} is SECOND in turn")
                    true
                }
                else -> {
                    android.util.Log.d("OnlineGameActivity", "onCardClick: Already 2 cards in this turn (first=$firstCardThisTurn, second=$secondCardThisTurn), BLOCKING click")
                    false
                }
            }
        }
        
        // Если не смогли занять слот - выходим
        if (!slotAssigned) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val success = firebaseManager.makeMove(roomId, card.id)
                
                if (success) {
                    soundManager.playSound(SoundManager.SoundType.CARD_FLIP)
                    android.util.Log.d("OnlineGameActivity", "Card clicked: id=${card.id}, move accepted")
                } else {
                    android.util.Log.d("OnlineGameActivity", "Move rejected by server for card ${card.id}")
                    
                    // Откатываем слот если сервер отклонил ход
                    synchronized(this@OnlineGameActivity) {
                        if (secondCardThisTurn == card.id) {
                            secondCardThisTurn = null
                        } else if (firstCardThisTurn == card.id) {
                            firstCardThisTurn = null
                        }
                        pendingClicks.remove(card.id)
                    }
                    
                    Toast.makeText(this@OnlineGameActivity, "Сейчас не ваш ход", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("OnlineGameActivity", "Error making move for card ${card.id}", e)
                
                // Откатываем слот при ошибке
                synchronized(this@OnlineGameActivity) {
                    if (secondCardThisTurn == card.id) {
                        secondCardThisTurn = null
                    } else if (firstCardThisTurn == card.id) {
                        firstCardThisTurn = null
                    }
                    pendingClicks.remove(card.id)
                }
                
                Toast.makeText(
                    this@OnlineGameActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getImageResource(pairId: Int): Int {
        // Маппинг pairId на реальные drawable ресурсы
        return when (pairId % 14) {
            0 -> R.drawable.card1
            1 -> R.drawable.card2
            2 -> R.drawable.card3
            3 -> R.drawable.card4
            4 -> R.drawable.card5
            5 -> R.drawable.card6
            6 -> R.drawable.card7
            7 -> R.drawable.card8
            8 -> R.drawable.card9
            9 -> R.drawable.card10
            10 -> R.drawable.card11
            11 -> R.drawable.card12
            12 -> R.drawable.card13
            13 -> R.drawable.card14
            else -> R.drawable.card1
        }
    }

    private fun showResultDialog(room: OnlineGameRoom) {
        currentDialog?.dismiss()
        
        // Останавливаем все таймеры
        timerRunnable?.let { handler.removeCallbacks(it) }
        turnTimerRunnable?.let { handler.removeCallbacks(it) }
        
        // Определяем победителя
        val isMyWin = if (myPlayerId == room.hostPlayerId) {
            room.hostPairs > room.guestPairs
        } else {
            room.guestPairs > room.hostPairs
        }
        
        val isDraw = room.hostPairs == room.guestPairs
        
        // Сохраняем статистику
        if (isMyWin) {
            val stars = 3
            
            statsManager.saveGameResult(
                mode = StatsManager.MODE_ONLINE,
                levelId = level,
                won = true,
                time = 0,
                moves = 0,
                stars = stars
            )
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_online_result, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Устанавливаем результат
        val tvResult = dialogView.findViewById<android.widget.TextView>(R.id.tvResult)
        when {
            isDraw -> {
                tvResult.text = "НИЧЬЯ!"
                tvResult.setTextColor(getColor(R.color.player_turn_inactive))
            }
            isMyWin -> {
                tvResult.text = "ПОБЕДА!"
                tvResult.setTextColor(getColor(android.R.color.holo_green_dark))
            }
            else -> {
                tvResult.text = "ПОРАЖЕНИЕ"
                tvResult.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
        
        // Устанавливаем счет
        dialogView.findViewById<android.widget.TextView>(R.id.tvPlayer1Score).text = 
            "Игрок 1: ${room.hostPairs} пар"
        dialogView.findViewById<android.widget.TextView>(R.id.tvPlayer2Score).text = 
            "Игрок 2: ${room.guestPairs} пар"
        
        dialogView.findViewById<android.widget.ImageButton>(R.id.btnMenu).setOnClickListener {
            currentDialog?.dismiss()
            currentDialog = null
            finish()
        }
        
        currentDialog = dialog
        dialog.show()
    }

    private fun showGameEndedDialog(message: String) {
        currentDialog?.dismiss()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_confirm, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Меняем текст
        dialogView.findViewById<android.widget.TextView>(R.id.tvTitle).text = "ИГРА ЗАВЕРШЕНА"
        dialogView.findViewById<android.widget.TextView>(R.id.tvMessage).text = message
        
        // Скрываем кнопку "Остаться", показываем только "OK"
        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).visibility = android.view.View.GONE
        dialogView.findViewById<android.widget.Button>(R.id.btnExit).apply {
            text = "OK"
            setBackgroundColor(getColor(R.color.player_turn_active))
            setOnClickListener {
                currentDialog?.dismiss()
                currentDialog = null
                finish()
            }
        }
        
        currentDialog = dialog
        dialog.show()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Показываем диалог подтверждения вместо немедленного выхода
        showExitConfirmDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Останавливаем таймеры
        timerRunnable?.let { handler.removeCallbacks(it) }
        turnTimerRunnable?.let { handler.removeCallbacks(it) }
        
        // Закрываем диалог, если он открыт
        currentDialog?.dismiss()
        currentDialog = null
        
        soundManager.release()
        
        // Покидаем комнату при выходе
        lifecycleScope.launch {
            try {
                firebaseManager.leaveRoom(roomId)
            } catch (e: Exception) {
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "room_id"
        const val EXTRA_IS_HOST = "is_host"
        const val EXTRA_LEVEL = "level"
    }
}
