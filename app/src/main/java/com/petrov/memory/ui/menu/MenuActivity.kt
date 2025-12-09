package com.petrov.memory.ui.menu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.databinding.ActivityMenuBinding
import com.petrov.memory.ui.game.GameActivity

/**
 * Главное меню игры
 * Из ТЗ раздел 4.1.1.2 - Подсистема пользовательского интерфейса
 */
class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        // Кнопка "Играть" - открывает выбор режима
        binding.btnPlay.setOnClickListener {
            startActivity(Intent(this, com.petrov.memory.ui.mode.ModeSelectionActivity::class.java))
        }

        // Кнопка "Настройки"
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, com.petrov.memory.ui.settings.SettingsActivity::class.java))
        }

        // Кнопка "Статистика"
        binding.btnStatistics.setOnClickListener {
            startActivity(Intent(this, com.petrov.memory.ui.statistics.StatisticsActivity::class.java))
        }

        // Кнопка "Выход"
        binding.btnExit.setOnClickListener {
            finish()
        }
    }
}
