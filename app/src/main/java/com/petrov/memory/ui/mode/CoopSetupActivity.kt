package com.petrov.memory.ui.mode

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.R
import com.petrov.memory.databinding.ActivityCoopSetupBinding
import com.petrov.memory.domain.model.TimerMode
import com.petrov.memory.ui.game.CoopGameActivity

/**
 * Экран настройки кооперативной игры
 * Из ЛР №4: Форма "Настройка уровня" - выбор таймера и лимита времени
 */
class CoopSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoopSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoopSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTimerModeListener()
        setupButtons()
    }

    /**
     * Слушатель переключения режима таймера
     * Показывает/скрывает выбор лимита времени
     */
    private fun setupTimerModeListener() {
        binding.rgTimerMode.setOnCheckedChangeListener { _, checkedId ->
            val showTimerLimit = checkedId == R.id.rbWithTimer
            binding.tvTimerLimitLabel.visibility = if (showTimerLimit) View.VISIBLE else View.GONE
            binding.rgTimerLimit.visibility = if (showTimerLimit) View.VISIBLE else View.GONE
        }
    }

    private fun setupButtons() {
        // Начать игру
        binding.btnStartGame.setOnClickListener {
            startCooperativeGame()
        }

        // Назад
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * Запуск кооперативной игры с выбранными параметрами
     */
    private fun startCooperativeGame() {
        // Получаем количество пар
        val pairs = when (binding.rgPairs.checkedRadioButtonId) {
            R.id.rbPairs4 -> 4
            R.id.rbPairs6 -> 6
            R.id.rbPairs9 -> 9
            else -> 4
        }

        // Получаем режим таймера
        val timerMode = when (binding.rgTimerMode.checkedRadioButtonId) {
            R.id.rbWithTimer -> TimerMode.WITH_TIMER
            R.id.rbWithoutTimer -> TimerMode.WITHOUT_TIMER
            else -> TimerMode.WITHOUT_TIMER
        }

        // Получаем лимит времени (если включен таймер)
        val timerLimit = if (timerMode == TimerMode.WITH_TIMER) {
            when (binding.rgTimerLimit.checkedRadioButtonId) {
                R.id.rbLimit60 -> 60
                R.id.rbLimit90 -> 90
                R.id.rbLimit120 -> 120
                else -> 60
            }
        } else {
            0
        }

        // Запускаем игру
        val intent = Intent(this, CoopGameActivity::class.java).apply {
            putExtra(EXTRA_PAIRS_COUNT, pairs)
            putExtra(EXTRA_TIMER_MODE, timerMode.name)
            putExtra(EXTRA_TIMER_LIMIT, timerLimit)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_PAIRS_COUNT = "pairs_count"
        const val EXTRA_TIMER_MODE = "timer_mode"
        const val EXTRA_TIMER_LIMIT = "timer_limit"
    }
}
