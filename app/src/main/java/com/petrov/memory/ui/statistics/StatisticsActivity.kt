package com.petrov.memory.ui.statistics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petrov.memory.databinding.ActivityStatisticsBinding

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
