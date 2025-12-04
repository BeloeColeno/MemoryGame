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
        binding.btnEasy.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
