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
import com.petrov.memory.domain.model.OnlineGameRoom
import com.petrov.memory.domain.model.TimerMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
        roomsAdapter = RoomsAdapter { room -> joinRoom(room)
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_room, null)

        val rgLevel = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgLevel)
        val rgTimer = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgTimer)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Обработчик кнопки "Создать"
        dialogView.findViewById<android.widget.Button>(R.id.btnCreate).setOnClickListener {
            selectedLevel = when (rgLevel.checkedRadioButtonId) {
                R.id.rbLevel1 -> 1
                R.id.rbLevel2 -> 2
                R.id.rbLevel3 -> 3
                else -> 1
            }

            when (rgTimer.checkedRadioButtonId) {
                R.id.rbNoTimer -> {
                    selectedTimerMode = TimerMode.WITHOUT_TIMER
                    selectedTimeLimit = null
                }
                R.id.rbTimer60 -> {
                    selectedTimerMode = TimerMode.WITH_TIMER
                    selectedTimeLimit = 60
                }
                R.id.rbTimer90 -> {
                    selectedTimerMode = TimerMode.WITH_TIMER
                    selectedTimeLimit = 90
                }
                R.id.rbTimer120 -> {
                    selectedTimerMode = TimerMode.WITH_TIMER
                    selectedTimeLimit = 120
                }
                else -> {
                    selectedTimerMode = TimerMode.WITHOUT_TIMER
                    selectedTimeLimit = null
                }
            }
            
            dialog.dismiss()
            createRoom()
        }
        
        // Обработчик кнопки "Отмена"
        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun createRoom() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                android.util.Log.d("OnlineLobby", "Creating room: level=$selectedLevel, timer=$selectedTimerMode, limit=$selectedTimeLimit")
                
                val roomId = withTimeout(10000) { // 10 секунд timeout
                    firebaseManager.createRoom(
                        selectedLevel,
                        selectedTimerMode,
                        selectedTimeLimit
                    )
                }
                
                android.util.Log.d("OnlineLobby", "Room created: $roomId")

                val intent = Intent(this@OnlineLobbyActivity, OnlineWaitingRoomActivity::class.java)
                intent.putExtra(EXTRA_ROOM_ID, roomId)
                intent.putExtra(EXTRA_IS_HOST, true)
                intent.putExtra(EXTRA_LEVEL, selectedLevel)
                startActivity(intent)
                finish()
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("OnlineLobby", "Timeout creating room", e)
                Toast.makeText(
                    this@OnlineLobbyActivity,
                    "Превышено время ожидания. Проверьте интернет.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                android.util.Log.e("OnlineLobby", "Error creating room", e)
                Toast.makeText(
                    this@OnlineLobbyActivity,
                    "Ошибка создания комнаты: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun joinRoom(room: OnlineGameRoom) {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val success = firebaseManager.joinRoom(room.roomId)
                
                if (success) {
                    android.util.Log.d("OnlineLobbyActivity", "Joining room: id=${room.roomId}, level=${room.level}")

                    val intent = Intent(this@OnlineLobbyActivity, OnlineWaitingRoomActivity::class.java)
                    intent.putExtra(EXTRA_ROOM_ID, room.roomId)
                    intent.putExtra(EXTRA_IS_HOST, false)
                    intent.putExtra(EXTRA_LEVEL, room.level)  // КРИТИЧНО: Передаем уровень из комнаты!
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
