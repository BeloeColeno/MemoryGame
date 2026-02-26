package com.petrov.memory.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.petrov.memory.data.Achievement
import com.petrov.memory.data.AchievementManager

class AchievementPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("achievements", Context.MODE_PRIVATE)
    
    fun unlockAchievement(achievementId: Int) {
        prefs.edit().apply {
            putBoolean("achievement_$achievementId", true)
            putLong("achievement_${achievementId}_date", System.currentTimeMillis())
            apply()
        }
    }
    
    fun isAchievementUnlocked(achievementId: Int): Boolean {
        return prefs.getBoolean("achievement_$achievementId", false)
    }
    
    fun getUnlockedDate(achievementId: Int): Long {
        return prefs.getLong("achievement_${achievementId}_date", 0L)
    }
    
    fun getAllAchievements(): List<Achievement> {
        return AchievementManager.getAllAchievements().map { achievement ->
            achievement.copy(
                isUnlocked = isAchievementUnlocked(achievement.id),
                unlockedDate = getUnlockedDate(achievement.id)
            )
        }
    }
    
    fun getUnlockedCount(): Int {
        return getAllAchievements().count { it.isUnlocked }
    }
    
    fun checkAndUnlockAchievement(achievementId: Int, condition: Boolean) {
        if (condition && !isAchievementUnlocked(achievementId)) {
            unlockAchievement(achievementId)
        }
    }
    
    // Вспомогательные методы для проверки условий достижений
    fun incrementGamesPlayed() {
        val count = prefs.getInt("games_played", 0) + 1
        prefs.edit().putInt("games_played", count).apply()
    }
    
    fun getGamesPlayed(): Int = prefs.getInt("games_played", 0)
    
    fun incrementCoopWins() {
        val count = prefs.getInt("coop_wins", 0) + 1
        prefs.edit().putInt("coop_wins", count).apply()
    }
    
    fun getCoopWins(): Int = prefs.getInt("coop_wins", 0)
    
    fun incrementPerfectGames() {
        val count = prefs.getInt("perfect_games", 0) + 1
        prefs.edit().putInt("perfect_games", count).apply()
    }
    
    fun getPerfectGames(): Int = prefs.getInt("perfect_games", 0)
    
    fun resetPerfectStreak() {
        prefs.edit().putInt("perfect_games", 0).apply()
    }
    
    fun addPlayTime(seconds: Long) {
        val total = prefs.getLong("total_play_time", 0) + seconds
        prefs.edit().putLong("total_play_time", total).apply()
    }
    
    fun getTotalPlayTime(): Long = prefs.getLong("total_play_time", 0)
    
    fun setLevelCompleted(level: Int) {
        prefs.edit().putBoolean("level_${level}_completed", true).apply()
    }
    
    fun areAllLevelsCompleted(): Boolean {
        return (1..5).all { prefs.getBoolean("level_${it}_completed", false) }
    }
}
