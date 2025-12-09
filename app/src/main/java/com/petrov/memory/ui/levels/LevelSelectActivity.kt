package com.petrov.memory.ui.levels

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.databinding.ActivityLevelSelectBinding
import com.petrov.memory.ui.game.GameActivity

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLevelSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLevelSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
    }

    private fun setupButtons() {
        // Легкий уровень: 4 пары, сетка 4x2
        binding.btnEasy.setOnClickListener {
            startGame(levelId = 1, pairs = 4, columns = 4)
        }

        // Средний уровень: 6 пар, сетка 4x3
        binding.btnMedium.setOnClickListener {
            startGame(levelId = 2, pairs = 6, columns = 4)
        }

        // Сложный уровень: 10 пар, сетка динамическая
        binding.btnHard.setOnClickListener {
            startGame(levelId = 3, pairs = 10, columns = 4)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun startGame(levelId: Int, pairs: Int, columns: Int) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_LEVEL_ID, levelId)
            putExtra(GameActivity.EXTRA_TOTAL_PAIRS, pairs)
            putExtra(GameActivity.EXTRA_GRID_COLUMNS, columns)
        }
        startActivity(intent)
    }
}
