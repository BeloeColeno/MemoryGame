package com.petrov.memory.ui.online

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.petrov.memory.R
import com.petrov.memory.data.firebase.FirebaseGameManager
import com.petrov.memory.databinding.ActivityOnlineLobbyBinding
import com.petrov.memory.domain.model.TimerMode
import kotlinx.coroutines.launch

/**
 * Экран онлайн-лобби
 * Создание комнаты или присоединение к существующей
 */
class OnlineLobbyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnlineLobbyBinding
    private val firebaseManager = FirebaseGameManager.getInstance()
    private lateinit var roomsAdapter: RoomsAdapter
    
    private var selectedLevel: Int = 1
    private var selectedTimerMode: TimerMode = TimerMode.WITHOUT_TIMER
    private var selectedTimeLimit: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnlineLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        loadAvailableRooms()
    }

    private fun setupRecyclerView() {
        roomsAdapter = RoomsAdapter { room ->
            joinRoom(room.roomId)
        }
        
        binding.recyclerRooms.apply {
            layoutManager = LinearLayoutManager(this@OnlineLobbyActivity)
            adapter = roomsAdapter
        }
    }

    private fun setupButtons() {
        binding.btnCreateRoom.setOnClickListener {
            showCreateRoomDialog()
        }
        
        binding.btnRefresh.setOnClickListener {
            loadAvailableRooms()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showCreateRoomDialog() {
        val levels = arrayOf("Уровень 1 (4 пары)", "Уровень 2 (6 пар)", "Уровень 3 (9 пар)")
        val timerModes = arrayOf("Без таймера", "С таймером (60 сек)", "С таймером (90 сек)", "С таймером (120 сек)")
        
        var selectedLevelIndex = 0
        var selectedTimerIndex = 0
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_room, null)
        
        AlertDialog.Builder(this)
            .setTitle("Создать комнату")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                selectedLevel = selectedLevelIndex + 1
                
                when (selectedTimerIndex) {
                    0 -> {
                        selectedTimerMode = TimerMode.WITHOUT_TIMER
                        selectedTimeLimit = null
                    }
                    1 -> {
                        selectedTimerMode = TimerMode.WITH_TIMER
                        selectedTimeLimit = 60
                    }
                    2 -> {
                        selectedTimerMode = TimerMode.WITH_TIMER
                        selectedTimeLimit = 90
                    }
                    3 -> {
                        selectedTimerMode = TimerMode.WITH_TIMER
                        selectedTimeLimit = 120
                    }
                }
                
                createRoom()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createRoom() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val roomId = firebaseManager.createRoom(
                    selectedLevel,
                    selectedTimerMode,
                    selectedTimeLimit
                )
                
                // Переходим в комнату ожидания
                val intent = Intent(this@OnlineLobbyActivity, OnlineWaitingRoomActivity::class.java)
                intent.putExtra(EXTRA_ROOM_ID, roomId)
                intent.putExtra(EXTRA_IS_HOST, true)
                intent.putExtra(EXTRA_LEVEL, selectedLevel)
                startActivity(intent)
                finish()
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnlineLobbyActivity,
                    "Ошибка создания комнаты: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun joinRoom(roomId: String) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val success = firebaseManager.joinRoom(roomId)
                
                if (success) {
                    // Переходим в комнату ожидания
                    val intent = Intent(this@OnlineLobbyActivity, OnlineWaitingRoomActivity::class.java)
                    intent.putExtra(EXTRA_ROOM_ID, roomId)
                    intent.putExtra(EXTRA_IS_HOST, false)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@OnlineLobbyActivity,
                        "Не удалось присоединиться к комнате",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnlineLobbyActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadAvailableRooms() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoRooms.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val rooms = firebaseManager.findAvailableRooms()
                
                if (rooms.isEmpty()) {
                    binding.tvNoRooms.visibility = View.VISIBLE
                    roomsAdapter.submitList(emptyList())
                } else {
                    roomsAdapter.submitList(rooms)
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@OnlineLobbyActivity,
                    "Ошибка загрузки комнат: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "room_id"
        const val EXTRA_IS_HOST = "is_host"
        const val EXTRA_LEVEL = "level"
    }
}
