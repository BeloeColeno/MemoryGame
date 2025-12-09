package com.petrov.memory.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.databinding.ActivitySettingsBinding
import com.petrov.memory.data.preferences.SettingsManager

/**
 * Экран настроек приложения
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        
        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        // Кнопка "Назад"
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // Переключатель звука
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.isSoundEnabled = isChecked
        }
        
        // Переключатель вибрации
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.isVibrationEnabled = isChecked
        }
    }

    private fun loadSettings() {
        // Загружаем сохраненные настройки
        binding.switchSound.isChecked = settingsManager.isSoundEnabled
        binding.switchVibration.isChecked = settingsManager.isVibrationEnabled
    }
}
