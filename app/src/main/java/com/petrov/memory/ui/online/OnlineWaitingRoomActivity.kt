package com.petrov.memory.ui.online

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.petrov.memory.data.firebase.FirebaseGameManager
import com.petrov.memory.databinding.ActivityOnlineWaitingRoomBinding
import kotlinx.coroutines.launch

/**
 * Экран ожидания в онлайн-комнате
 * Хост ждет второго игрока, гость ждет начала игры
 */
class OnlineWaitingRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlineWaitingRoomBinding
    private val firebaseManager = FirebaseGameManager.getInstance()
    
    private lateinit var roomId: String
    private var isHost: Boolean = false
    private var level: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineWaitingRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getStringExtra(OnlineLobbyActivity.EXTRA_ROOM_ID) ?: run {
            finish()
            return
        }
        
        isHost = intent.getBooleanExtra(OnlineLobbyActivity.EXTRA_IS_HOST, false)
        level = intent.getIntExtra(OnlineLobbyActivity.EXTRA_LEVEL, 1)

        setupUI()
        observeRoom()
    }

    private fun setupUI() {
        binding.tvRoomId.text = "ID комнаты: ${roomId.take(8)}"
        
        if (isHost) {
            binding.tvStatus.text = "Ожидание второго игрока..."
            binding.btnStart.isEnabled = false
            binding.btnStart.setOnClickListener {
                startGame()
            }
        } else {
            binding.tvStatus.text = "Ожидание начала игры..."
            binding.btnStart.isEnabled = false
            binding.btnStart.text = "Ожидайте..."
        }
        
        binding.btnCancel.setOnClickListener {
            leaveRoom()
        }
    }

    private fun observeRoom() {
        lifecycleScope.launch {
            firebaseManager.observeRoom(roomId).collect { room ->
                if (room == null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@OnlineWaitingRoomActivity,
                            "Комната закрыта",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    return@collect
                }
                
                // Проверяем, присоединился ли второй игрок
                if (isHost && room.guestPlayerId != null) {
                    runOnUiThread {
                        binding.tvStatus.text = "Второй игрок присоединился!"
                        binding.btnStart.isEnabled = true
                    }
                }
                
                // Проверяем, началась ли игра
                if (room.gameStarted) {
                    runOnUiThread {
                        startOnlineGame()
                    }
                }
            }
        }
    }

    private fun startGame() {
        lifecycleScope.launch {
            try {
                val success = firebaseManager.startGame(roomId)
                if (!success) {
                    Toast.makeText(
                        this@OnlineWaitingRoomActivity,
                        "Не удалось начать игру",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnlineWaitingRoomActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startOnlineGame() {
        val intent = Intent(this, OnlineGameActivity::class.java)
        intent.putExtra(OnlineGameActivity.EXTRA_ROOM_ID, roomId)
        intent.putExtra(OnlineGameActivity.EXTRA_IS_HOST, isHost)
        intent.putExtra(OnlineGameActivity.EXTRA_LEVEL, level)
        startActivity(intent)
        finish()
    }

    private fun leaveRoom() {
        lifecycleScope.launch {
            try {
                firebaseManager.leaveRoom(roomId)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnlineWaitingRoomActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        leaveRoom()
    }
}
