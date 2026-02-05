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
import com.petrov.memory.databinding.ActivityOnlineGameBinding
import com.petrov.memory.domain.model.Card
import com.petrov.memory.domain.model.OnlineCard
import com.petrov.memory.domain.model.OnlineGameRoom
import com.petrov.memory.ui.game.CardsAdapter
import com.petrov.memory.util.SoundManager
import com.petrov.memory.util.VibrationManager
import kotlinx.coroutines.launch

/**
 * Экран онлайн-игры через Firebase
 */
class OnlineGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlineGameBinding
    private val firebaseManager = FirebaseGameManager.getInstance()
    
    private lateinit var roomId: String
    private var isHost: Boolean = false
    private var level: Int = 1
    private var myPlayerId: String = ""
    
    private lateinit var cardsAdapter: CardsAdapter
    private val cards = mutableListOf<Card>()
    
    private var firstFlippedCard: Card? = null
    private var secondFlippedCard: Card? = null
    private var isProcessing = false
    
    private lateinit var soundManager: SoundManager
    private lateinit var vibrationManager: VibrationManager

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

        lifecycleScope.launch {
            myPlayerId = firebaseManager.getCurrentPlayerId()
            setupGame()
            observeRoom()
        }
    }

    private fun setupGame() {
        val (columns, _) = getGridSize(level)
        
        // Вычисляем доступное пространство для карточек
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density
        
        val topBottomReserved = (density * 140).toInt()
        val sideMargins = (density * 32).toInt()
        val cardPadding = (density * 2).toInt()
        val effectiveGap = cardPadding * 2
        
        val availableWidth = screenWidth - sideMargins
        val availableHeight = screenHeight - topBottomReserved
        
        cardsAdapter = CardsAdapter(
            cards = emptyList(),
            availableWidth = availableWidth,
            availableHeight = availableHeight,
            gap = effectiveGap,
            columns = columns
        ) { position ->
            val card = cards.getOrNull(position)
            if (card != null) {
                onCardClick(card)
            }
        }
        
        binding.recyclerCards.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(this@OnlineGameActivity, columns)
            adapter = cardsAdapter
        }
    }

    private fun getGridSize(level: Int): Pair<Int, Int> {
        return when (level) {
            1 -> Pair(4, 2) // 4 пары
            2 -> Pair(4, 3) // 6 пар
            3 -> Pair(6, 3) // 9 пар
            else -> Pair(4, 2)
        }
    }

    private fun observeRoom() {
        lifecycleScope.launch {
            firebaseManager.observeRoom(roomId).collect { room ->
                if (room == null) {
                    showGameEndedDialog("Игра завершена")
                    return@collect
                }
                
                updateUI(room)
                
                if (room.gameFinished) {
                    showResultDialog(room)
                }
            }
        }
    }

    private fun updateUI(room: OnlineGameRoom) {
        // Обновляем карточки
        if (cards.isEmpty()) {
            cards.addAll(room.cards.map { onlineCard ->
                Card(
                    id = onlineCard.id,
                    imageResId = getImageResource(onlineCard.imageResId),
                    isRevealed = onlineCard.isFlipped,
                    isMatched = onlineCard.isMatched
                )
            })
            cardsAdapter.updateCards(cards.toList())
        } else {
            // Обновляем существующие карточки
            room.cards.forEachIndexed { index, onlineCard ->
                if (index < cards.size) {
                    cards[index].isRevealed = onlineCard.isFlipped
                    cards[index].isMatched = onlineCard.isMatched
                }
            }
            cardsAdapter.updateCards(cards.toList())
        }
        
        // Обновляем счет
        binding.tvHostScore.text = "Игрок 1: ${room.hostPairs} пар"
        binding.tvGuestScore.text = "Игрок 2: ${room.guestPairs} пар"
        
        // Индикатор хода
        val isMyTurn = room.currentPlayerId == myPlayerId
        binding.tvTurnIndicator.text = if (isMyTurn) {
            "Ваш ход"
        } else {
            "Ход противника"
        }
        
        // Подсветка текущего игрока
        if (isMyTurn) {
            binding.viewTurnIndicator.setBackgroundResource(R.color.player_turn_active)
        } else {
            binding.viewTurnIndicator.setBackgroundResource(R.color.player_turn_inactive)
        }
    }

    private fun onCardClick(card: Card) {
        if (isProcessing || card.isMatched || card.isRevealed) return
        
        lifecycleScope.launch {
            try {
                val success = firebaseManager.makeMove(roomId, card.id)
                
                if (success) {
                    soundManager.playSound(SoundManager.SoundType.CARD_FLIP)
                    
                    if (firstFlippedCard == null) {
                        firstFlippedCard = card
                    } else if (secondFlippedCard == null && firstFlippedCard?.id != card.id) {
                        secondFlippedCard = card
                        isProcessing = true
                        
                        // Задержка для просмотра карточек
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkMatch()
                        }, 1000)
                    }
                } else {
                    Toast.makeText(this@OnlineGameActivity, "Сейчас не ваш ход", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnlineGameActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkMatch() {
        val card1 = firstFlippedCard
        val card2 = secondFlippedCard
        
        if (card1 != null && card2 != null) {
            lifecycleScope.launch {
                try {
                    firebaseManager.checkMatch(roomId, card1.id, card2.id)
                    
                    firstFlippedCard = null
                    secondFlippedCard = null
                    isProcessing = false
                    
                } catch (e: Exception) {
                    Toast.makeText(
                        this@OnlineGameActivity,
                        "Ошибка: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isProcessing = false
                }
            }
        }
    }

    private fun getImageResource(resourceId: Int): Int {
        // Здесь должна быть логика получения реального ресурса изображения
        // Пока возвращаем placeholder
        return R.drawable.cover // Заглушка
    }

    private fun showResultDialog(room: OnlineGameRoom) {
        val winner = when {
            room.hostPairs > room.guestPairs -> "Игрок 1"
            room.guestPairs > room.hostPairs -> "Игрок 2"
            else -> "Ничья"
        }
        
        val message = """
            Игра завершена!
            
            Игрок 1: ${room.hostPairs} пар
            Игрок 2: ${room.guestPairs} пар
            
            Победитель: $winner
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Результаты")
            .setMessage(message)
            .setPositiveButton("В меню") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showGameEndedDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Игра завершена")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        
        // Покидаем комнату при выходе
        lifecycleScope.launch {
            try {
                firebaseManager.leaveRoom(roomId)
            } catch (e: Exception) {
                // Игнорируем ошибки при выходе
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "room_id"
        const val EXTRA_IS_HOST = "is_host"
        const val EXTRA_LEVEL = "level"
    }
}
