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
        // Кнопка "Играть"
        binding.btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        // Кнопка "Настройки" (пока заглушка)
        binding.btnSettings.setOnClickListener {
            // TODO: Открыть экран настроек
        }

        // Кнопка "Статистика" (пока заглушка)
        binding.btnStatistics.setOnClickListener {
            // TODO: Открыть экран статистики
        }

        // Кнопка "Выход"
        binding.btnExit.setOnClickListener {
            finish()
        }
    }
}
