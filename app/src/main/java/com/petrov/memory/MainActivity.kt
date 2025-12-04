package com.petrov.memory

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.ui.game.GameActivity

/**
 * Главная Activity - временно запускает игру сразу
 * TODO: В будущем здесь будет меню (из ТЗ раздел 4.1.1.2)
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Пока сразу запускаем игру для тестирования
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }
}