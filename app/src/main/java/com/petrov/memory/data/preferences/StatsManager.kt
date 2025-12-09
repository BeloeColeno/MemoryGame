package com.petrov.memory.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.petrov.memory.domain.model.GameStats
import com.petrov.memory.domain.model.LevelStats

/**
 * Менеджер для работы со статистикой через SharedPreferences
 * Из ТЗ раздел 4.1.1.3 - Подсистема сохранения данных
 */
class StatsManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "memory_game_stats"
        
        // Общие ключи
        private const val KEY_TOTAL_GAMES = "total_games"
        private const val KEY_TOTAL_WINS = "total_wins"
        private const val KEY_TOTAL_STARS = "total_stars"
        private const val KEY_TOTAL_TIME = "total_time"
        
        // Ключи для уровней (префикс level_X_)
        private fun keyGamesPlayed(levelId: Int) = "level_${levelId}_games_played"
        private fun keyGamesWon(levelId: Int) = "level_${levelId}_games_won"
        private fun keyBestTime(levelId: Int) = "level_${levelId}_best_time"
        private fun keyBestMoves(levelId: Int) = "level_${levelId}_best_moves"
        private fun keyBestStars(levelId: Int) = "level_${levelId}_best_stars"
        private fun keyTotalStars(levelId: Int) = "level_${levelId}_total_stars"
    }
    
    /**
     * Получить статистику по уровню
     */
    fun getLevelStats(levelId: Int): LevelStats {
        return LevelStats(
            levelId = levelId,
            gamesPlayed = prefs.getInt(keyGamesPlayed(levelId), 0),
            gamesWon = prefs.getInt(keyGamesWon(levelId), 0),
            bestTime = prefs.getInt(keyBestTime(levelId), Int.MAX_VALUE),
            bestMoves = prefs.getInt(keyBestMoves(levelId), Int.MAX_VALUE),
            bestStars = prefs.getInt(keyBestStars(levelId), 0),
            totalStars = prefs.getInt(keyTotalStars(levelId), 0)
        )
    }
    
    /**
     * Получить общую статистику
     */
    fun getGameStats(): GameStats {
        return GameStats(
            totalGamesPlayed = prefs.getInt(KEY_TOTAL_GAMES, 0),
            totalGamesWon = prefs.getInt(KEY_TOTAL_WINS, 0),
            totalStars = prefs.getInt(KEY_TOTAL_STARS, 0),
            totalTime = prefs.getInt(KEY_TOTAL_TIME, 0),
            level1 = getLevelStats(1),
            level2 = getLevelStats(2),
            level3 = getLevelStats(3)
        )
    }
    
    /**
     * Сохранить результат игры
     */
    fun saveGameResult(levelId: Int, won: Boolean, time: Int, moves: Int, stars: Int) {
        prefs.edit().apply {
            // Обновляем общую статистику
            putInt(KEY_TOTAL_GAMES, prefs.getInt(KEY_TOTAL_GAMES, 0) + 1)
            if (won) {
                putInt(KEY_TOTAL_WINS, prefs.getInt(KEY_TOTAL_WINS, 0) + 1)
            }
            putInt(KEY_TOTAL_STARS, prefs.getInt(KEY_TOTAL_STARS, 0) + stars)
            putInt(KEY_TOTAL_TIME, prefs.getInt(KEY_TOTAL_TIME, 0) + time)
            
            // Обновляем статистику уровня
            putInt(keyGamesPlayed(levelId), prefs.getInt(keyGamesPlayed(levelId), 0) + 1)
            if (won) {
                putInt(keyGamesWon(levelId), prefs.getInt(keyGamesWon(levelId), 0) + 1)
            }
            putInt(keyTotalStars(levelId), prefs.getInt(keyTotalStars(levelId), 0) + stars)
            
            // Обновляем рекорды (только если выиграл)
            if (won) {
                val currentBestTime = prefs.getInt(keyBestTime(levelId), Int.MAX_VALUE)
                if (time < currentBestTime) {
                    putInt(keyBestTime(levelId), time)
                }
                
                val currentBestMoves = prefs.getInt(keyBestMoves(levelId), Int.MAX_VALUE)
                if (moves < currentBestMoves) {
                    putInt(keyBestMoves(levelId), moves)
                }
                
                val currentBestStars = prefs.getInt(keyBestStars(levelId), 0)
                if (stars > currentBestStars) {
                    putInt(keyBestStars(levelId), stars)
                }
            }
            
            apply()
        }
    }
    
    /**
     * Сбросить всю статистику
     */
    fun resetStats() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Сбросить статистику по уровню
     */
    fun resetLevelStats(levelId: Int) {
        prefs.edit().apply {
            remove(keyGamesPlayed(levelId))
            remove(keyGamesWon(levelId))
            remove(keyBestTime(levelId))
            remove(keyBestMoves(levelId))
            remove(keyBestStars(levelId))
            remove(keyTotalStars(levelId))
            apply()
        }
    }
}
