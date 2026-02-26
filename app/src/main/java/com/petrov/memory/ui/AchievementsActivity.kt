package com.petrov.memory.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.petrov.memory.R
import com.petrov.memory.data.preferences.AchievementPreferences

class AchievementsActivity : AppCompatActivity() {
    
    private lateinit var achievementPrefs: AchievementPreferences
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AchievementsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        
        achievementPrefs = AchievementPreferences(this)
        
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener { finish() }
        
        recyclerView = findViewById(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        
        val achievements = achievementPrefs.getAllAchievements()
        adapter = AchievementsAdapter(achievements)
        recyclerView.adapter = adapter
    }
}
