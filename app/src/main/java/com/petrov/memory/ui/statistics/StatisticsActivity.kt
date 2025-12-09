package com.petrov.memory.ui.statistics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.databinding.ActivityStatisticsBinding
import com.petrov.memory.data.preferences.StatsManager
import com.petrov.memory.domain.model.LevelStats

/**
 * Экран статистики
 * Отображает статистику по всем уровням
 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var statsManager: StatsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statsManager = StatsManager(this)
        
        setupUI()
        loadStats()
    }

    private fun setupUI() {
        // Кнопка "Назад"
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Кнопка "Сброс статистики"
        binding.btnReset.setOnClickListener {
            resetStats()
        }
    }

    private fun loadStats() {
        val stats = statsManager.getGameStats()
        
        // Общая статистика
        binding.tvTotalGames.text = stats.totalGamesPlayed.toString()
        binding.tvTotalWins.text = stats.totalGamesWon.toString()
        binding.tvTotalStars.text = stats.totalStars.toString()
        binding.tvTotalTime.text = formatTime(stats.totalTime)
        
        // Статистика по уровням
        // Уровень 1
        binding.layoutLevel1.tvLevelName.text = "Легкий (4 пары)"
        loadLevelStats(stats.level1, 
            binding.layoutLevel1.tvLevel1Games, 
            binding.layoutLevel1.tvLevel1BestTime, 
            binding.layoutLevel1.tvLevel1BestMoves, 
            binding.layoutLevel1.tvLevel1Stars)
        
        // Уровень 2
        binding.layoutLevel2.tvLevelName.text = "Средний (6 пар)"
        loadLevelStats(stats.level2, 
            binding.layoutLevel2.tvLevel1Games, 
            binding.layoutLevel2.tvLevel1BestTime, 
            binding.layoutLevel2.tvLevel1BestMoves, 
            binding.layoutLevel2.tvLevel1Stars)
        
        // Уровень 3
        binding.layoutLevel3.tvLevelName.text = "Сложный (9 пар)"
        loadLevelStats(stats.level3, 
            binding.layoutLevel3.tvLevel1Games, 
            binding.layoutLevel3.tvLevel1BestTime, 
            binding.layoutLevel3.tvLevel1BestMoves, 
            binding.layoutLevel3.tvLevel1Stars)
    }

    private fun loadLevelStats(
        levelStats: LevelStats,
        tvGames: android.widget.TextView,
        tvTime: android.widget.TextView,
        tvMoves: android.widget.TextView,
        tvStars: android.widget.TextView
    ) {
        tvGames.text = "${levelStats.gamesWon} / ${levelStats.gamesPlayed}"
        
        if (levelStats.bestTime != Int.MAX_VALUE) {
            tvTime.text = formatTime(levelStats.bestTime)
        } else {
            tvTime.text = "-"
        }
        
        if (levelStats.bestMoves != Int.MAX_VALUE) {
            tvMoves.text = levelStats.bestMoves.toString()
        } else {
            tvMoves.text = "-"
        }
        
        tvStars.text = "${levelStats.bestStars} ★"
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    private fun resetStats() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Сброс статистики")
            .setMessage("Вы уверены, что хотите сбросить всю статистику?")
            .setPositiveButton("Да") { _, _ ->
                statsManager.resetStats()
                loadStats()
                android.widget.Toast.makeText(this, "Статистика сброшена", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
