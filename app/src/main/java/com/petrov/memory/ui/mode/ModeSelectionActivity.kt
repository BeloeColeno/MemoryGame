package com.petrov.memory.ui.mode

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.databinding.ActivityModeSelectionBinding
import com.petrov.memory.domain.model.GameMode
import com.petrov.memory.ui.levels.LevelSelectActivity

/**
 * Экран выбора режима игры
 * Из ЛР №4: Форма "Выбор режима" - Одиночный / Совместный
 */
class ModeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        // Одиночная игра → выбор уровня
        binding.btnSinglePlayer.setOnClickListener {
            val intent = Intent(this, LevelSelectActivity::class.java)
            intent.putExtra(EXTRA_GAME_MODE, GameMode.SINGLE_PLAYER.name)
            startActivity(intent)
        }

        // Игра на двоих → настройка кооперативной игры
        binding.btnCooperative.setOnClickListener {
            val intent = Intent(this, CoopSetupActivity::class.java)
            startActivity(intent)
        }

        // Онлайн игра → лобби
        binding.btnOnline.setOnClickListener {
            val intent = Intent(this, com.petrov.memory.ui.online.OnlineLobbyActivity::class.java)
            startActivity(intent)
        }

        // Назад
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    companion object {
        const val EXTRA_GAME_MODE = "game_mode"
    }
}
