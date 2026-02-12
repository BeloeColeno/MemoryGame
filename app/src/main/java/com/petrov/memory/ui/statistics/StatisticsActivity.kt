package com.petrov.memory.ui.statistics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.R
import com.petrov.memory.data.preferences.StatsManager
import com.petrov.memory.domain.model.LevelStats
import com.petrov.memory.domain.model.ModeStats
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout

/**
 * Экран статистики с вкладками по режимам
 * Отображает статистику по офлайн, онлайн и кооперативному режимам
 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var statsManager: StatsManager
    private var currentMode = StatsManager.MODE_OFFLINE

    // Views
    private lateinit var btnOfflineTab: Button
    private lateinit var btnOnlineTab: Button
    private lateinit var btnCoopTab: Button
    private lateinit var tvTotalGames: TextView
    private lateinit var tvTotalWins: TextView
    private lateinit var tvTotalStars: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var layoutLevel1: LinearLayout
    private lateinit var layoutLevel2: LinearLayout
    private lateinit var layoutLevel3: LinearLayout
    private lateinit var btnReset: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics_tabs)

        statsManager = StatsManager(this)
        
        initViews()
        setupUI()
        loadStats()
    }

    private fun initViews() {
        btnOfflineTab = findViewById(R.id.btnOfflineTab)
        btnOnlineTab = findViewById(R.id.btnOnlineTab)
        btnCoopTab = findViewById(R.id.btnCoopTab)
        tvTotalGames = findViewById(R.id.tvTotalGames)
        tvTotalWins = findViewById(R.id.tvTotalWins)
        tvTotalStars = findViewById(R.id.tvTotalStars)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        layoutLevel1 = findViewById(R.id.layoutLevel1)
        layoutLevel2 = findViewById(R.id.layoutLevel2)
        layoutLevel3 = findViewById(R.id.layoutLevel3)
        btnReset = findViewById(R.id.btnReset)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupUI() {
        // Вкладки режимов
        btnOfflineTab.setOnClickListener {
            switchTab(StatsManager.MODE_OFFLINE)
        }
        
        btnOnlineTab.setOnClickListener {
            switchTab(StatsManager.MODE_ONLINE)
        }
        
        btnCoopTab.setOnClickListener {
            switchTab(StatsManager.MODE_COOP)
        }
        
        // Кнопка "Назад"
        btnBack.setOnClickListener {
            finish()
        }
        
        // Кнопка "Сброс статистики"
        btnReset.setOnClickListener {
            resetStats()
        }
    }

    private fun switchTab(mode: String) {
        currentMode = mode
        
        // Обновляем стиль вкладок
        val purpleActive = 0xFF9C27B0.toInt()
        val purpleLight = 0xFFE1BEE7.toInt()
        val white = 0xFFFFFFFF.toInt()
        
        btnOfflineTab.apply {
            if (mode == StatsManager.MODE_OFFLINE) {
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = android.content.res.ColorStateList.valueOf(purpleActive)
                setTextColor(white)
            } else {
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = android.content.res.ColorStateList.valueOf(purpleLight)
                setTextColor(purpleActive)
            }
        }
        
        btnOnlineTab.apply {
            if (mode == StatsManager.MODE_ONLINE) {
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = android.content.res.ColorStateList.valueOf(purpleActive)
                setTextColor(white)
            } else {
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = android.content.res.ColorStateList.valueOf(purpleLight)
                setTextColor(purpleActive)
            }
        }
        
        btnCoopTab.apply {
            if (mode == StatsManager.MODE_COOP) {
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = android.content.res.ColorStateList.valueOf(purpleActive)
                setTextColor(white)
            } else {
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = android.content.res.ColorStateList.valueOf(purpleLight)
                setTextColor(purpleActive)
            }
        }
        
        loadStats()
    }

    private fun loadStats() {
        val stats = statsManager.getModeStats(currentMode)
        
        // Общая статистика
        tvTotalGames.text = stats.gamesPlayed.toString()
        tvTotalWins.text = stats.gamesWon.toString()
        
        // Для онлайн режима не показываем звезды
        if (currentMode == StatsManager.MODE_ONLINE) {
            tvTotalStars.visibility = android.view.View.GONE
        } else {
            tvTotalStars.visibility = android.view.View.VISIBLE
            tvTotalStars.text = stats.totalStars.toString()
        }
        
        tvTotalTime.text = formatTime(stats.totalTime)
        
        // Статистика по уровням
        loadLevelStats(stats.level1, layoutLevel1, "Легкий (4 пары)")
        loadLevelStats(stats.level2, layoutLevel2, "Средний (6 пар)")
        loadLevelStats(stats.level3, layoutLevel3, "Сложный (9 пар)")
    }

    private fun loadLevelStats(levelStats: LevelStats, layout: LinearLayout, levelName: String) {
        layout.findViewById<TextView>(R.id.tvLevelName).text = levelName
        
        val tvGames = layout.findViewById<TextView>(R.id.tvLevel1Games)
        val tvTime = layout.findViewById<TextView>(R.id.tvLevel1BestTime)
        val tvMoves = layout.findViewById<TextView>(R.id.tvLevel1BestMoves)
        val tvStars = layout.findViewById<TextView>(R.id.tvLevel1Stars)
        
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
        
        // Для онлайн режима не показываем звезды
        if (currentMode == StatsManager.MODE_ONLINE) {
            tvStars.visibility = android.view.View.GONE
        } else {
            tvStars.visibility = android.view.View.VISIBLE
            tvStars.text = "${levelStats.bestStars} ★"
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%d:%02d", minutes, secs)
    }

    private fun resetStats() {
        val modeName = when (currentMode) {
            StatsManager.MODE_OFFLINE -> "офлайн режима"
            StatsManager.MODE_ONLINE -> "онлайн режима"
            StatsManager.MODE_COOP -> "кооперативного режима"
            else -> "текущего режима"
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Сброс статистики")
            .setMessage("Вы уверены, что хотите сбросить статистику $modeName?")
            .setPositiveButton("Да") { _, _ ->
                statsManager.resetModeStats(currentMode)
                loadStats()
                android.widget.Toast.makeText(
                    this,
                    "Статистика $modeName сброшена",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
