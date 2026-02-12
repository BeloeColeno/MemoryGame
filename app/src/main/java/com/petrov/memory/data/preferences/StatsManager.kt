package com.petrov.memory.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.petrov.memory.domain.model.GameStats
import com.petrov.memory.domain.model.LevelStats
import com.petrov.memory.domain.model.ModeStats

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
        
        // Режимы игры
        const val MODE_OFFLINE = "offline"
        const val MODE_ONLINE = "online"
        const val MODE_COOP = "coop"
        
        // Ключи для режимов и уровней (префикс mode_level_X_)
        private fun keyGamesPlayed(mode: String, levelId: Int) = "${mode}_level_${levelId}_games_played"
        private fun keyGamesWon(mode: String, levelId: Int) = "${mode}_level_${levelId}_games_won"
        private fun keyBestTime(mode: String, levelId: Int) = "${mode}_level_${levelId}_best_time"
        private fun keyBestMoves(mode: String, levelId: Int) = "${mode}_level_${levelId}_best_moves"
        private fun keyBestStars(mode: String, levelId: Int) = "${mode}_level_${levelId}_best_stars"
        private fun keyTotalStars(mode: String, levelId: Int) = "${mode}_level_${levelId}_total_stars"
        
        // Ключи для общей статистики режима
        private fun keyModeGames(mode: String) = "${mode}_total_games"
        private fun keyModeWins(mode: String) = "${mode}_total_wins"
        private fun keyModeStars(mode: String) = "${mode}_total_stars"
        private fun keyModeTime(mode: String) = "${mode}_total_time"
        
        // Старые ключи для миграции
        private fun keyGamesPlayedOld(levelId: Int) = "level_${levelId}_games_played"
        private fun keyGamesWonOld(levelId: Int) = "level_${levelId}_games_won"
        private fun keyBestTimeOld(levelId: Int) = "level_${levelId}_best_time"
        private fun keyBestMovesOld(levelId: Int) = "level_${levelId}_best_moves"
        private fun keyBestStarsOld(levelId: Int) = "level_${levelId}_best_stars"
        private fun keyTotalStarsOld(levelId: Int) = "level_${levelId}_total_stars"
    }
    
    init {
        // Миграция старых данных в офлайн режим (если есть)
        migrateOldStats()
    }
    
    /**
     * Миграция старой статистики в новый формат
     */
    private fun migrateOldStats() {
        if (prefs.contains(keyGamesPlayedOld(1)) && !prefs.contains(keyGamesPlayed(MODE_OFFLINE, 1))) {
            prefs.edit().apply {
                // Мигрируем уровни 1-3
                for (level in 1..3) {
                    val gamesPlayed = prefs.getInt(keyGamesPlayedOld(level), 0)
                    val gamesWon = prefs.getInt(keyGamesWonOld(level), 0)
                    val bestTime = prefs.getInt(keyBestTimeOld(level), Int.MAX_VALUE)
                    val bestMoves = prefs.getInt(keyBestMovesOld(level), Int.MAX_VALUE)
                    val bestStars = prefs.getInt(keyBestStarsOld(level), 0)
                    val totalStars = prefs.getInt(keyTotalStarsOld(level), 0)
                    
                    if (gamesPlayed > 0) {
                        putInt(keyGamesPlayed(MODE_OFFLINE, level), gamesPlayed)
                        putInt(keyGamesWon(MODE_OFFLINE, level), gamesWon)
                        putInt(keyBestTime(MODE_OFFLINE, level), bestTime)
                        putInt(keyBestMoves(MODE_OFFLINE, level), bestMoves)
                        putInt(keyBestStars(MODE_OFFLINE, level), bestStars)
                        putInt(keyTotalStars(MODE_OFFLINE, level), totalStars)
                    }
                }
                apply()
            }
        }
    }
    
    /**
     * Получить статистику по уровню для режима
     */
    fun getLevelStats(mode: String, levelId: Int): LevelStats {
        return LevelStats(
            levelId = levelId,
            gamesPlayed = prefs.getInt(keyGamesPlayed(mode, levelId), 0),
            gamesWon = prefs.getInt(keyGamesWon(mode, levelId), 0),
            bestTime = prefs.getInt(keyBestTime(mode, levelId), Int.MAX_VALUE),
            bestMoves = prefs.getInt(keyBestMoves(mode, levelId), Int.MAX_VALUE),
            bestStars = prefs.getInt(keyBestStars(mode, levelId), 0),
            totalStars = prefs.getInt(keyTotalStars(mode, levelId), 0)
        )
    }
    
    /**
     * Получить статистику по режиму
     */
    fun getModeStats(mode: String): ModeStats {
        return ModeStats(
            gamesPlayed = prefs.getInt(keyModeGames(mode), 0),
            gamesWon = prefs.getInt(keyModeWins(mode), 0),
            totalStars = prefs.getInt(keyModeStars(mode), 0),
            totalTime = prefs.getInt(keyModeTime(mode), 0),
            level1 = getLevelStats(mode, 1),
            level2 = getLevelStats(mode, 2),
            level3 = getLevelStats(mode, 3)
        )
    }
    
    /**
     * Получить общую статистику (для обратной совместимости)
     */
    fun getGameStats(): GameStats {
        val offlineStats = getModeStats(MODE_OFFLINE)
        return GameStats(
            totalGamesPlayed = prefs.getInt(KEY_TOTAL_GAMES, offlineStats.gamesPlayed),
            totalGamesWon = prefs.getInt(KEY_TOTAL_WINS, offlineStats.gamesWon),
            totalStars = prefs.getInt(KEY_TOTAL_STARS, offlineStats.totalStars),
            totalTime = prefs.getInt(KEY_TOTAL_TIME, offlineStats.totalTime),
            level1 = offlineStats.level1,
            level2 = offlineStats.level2,
            level3 = offlineStats.level3
        )
    }
    
    /**
     * Сохранить результат игры (старый метод для обратной совместимости)
     */
    fun saveGameResult(levelId: Int, won: Boolean, time: Int, moves: Int, stars: Int) {
        saveGameResult(MODE_OFFLINE, levelId, won, time, moves, stars)
    }
    
    /**
     * Сохранить результат игры с указанием режима
     */
    fun saveGameResult(mode: String, levelId: Int, won: Boolean, time: Int, moves: Int, stars: Int) {
        prefs.edit().apply {
            // Обновляем статистику режима
            putInt(keyModeGames(mode), prefs.getInt(keyModeGames(mode), 0) + 1)
            if (won) {
                putInt(keyModeWins(mode), prefs.getInt(keyModeWins(mode), 0) + 1)
            }
            putInt(keyModeStars(mode), prefs.getInt(keyModeStars(mode), 0) + stars)
            putInt(keyModeTime(mode), prefs.getInt(keyModeTime(mode), 0) + time)
            
            // Обновляем общую статистику
            putInt(KEY_TOTAL_GAMES, prefs.getInt(KEY_TOTAL_GAMES, 0) + 1)
            if (won) {
                putInt(KEY_TOTAL_WINS, prefs.getInt(KEY_TOTAL_WINS, 0) + 1)
            }
            putInt(KEY_TOTAL_STARS, prefs.getInt(KEY_TOTAL_STARS, 0) + stars)
            putInt(KEY_TOTAL_TIME, prefs.getInt(KEY_TOTAL_TIME, 0) + time)
            
            // Обновляем статистику уровня
            putInt(keyGamesPlayed(mode, levelId), prefs.getInt(keyGamesPlayed(mode, levelId), 0) + 1)
            if (won) {
                putInt(keyGamesWon(mode, levelId), prefs.getInt(keyGamesWon(mode, levelId), 0) + 1)
            }
            putInt(keyTotalStars(mode, levelId), prefs.getInt(keyTotalStars(mode, levelId), 0) + stars)
            
            // Обновляем рекорды (только если выиграл)
            if (won) {
                val currentBestTime = prefs.getInt(keyBestTime(mode, levelId), Int.MAX_VALUE)
                if (time < currentBestTime) {
                    putInt(keyBestTime(mode, levelId), time)
                }
                
                val currentBestMoves = prefs.getInt(keyBestMoves(mode, levelId), Int.MAX_VALUE)
                if (moves < currentBestMoves) {
                    putInt(keyBestMoves(mode, levelId), moves)
                }
                
                val currentBestStars = prefs.getInt(keyBestStars(mode, levelId), 0)
                if (stars > currentBestStars) {
                    putInt(keyBestStars(mode, levelId), stars)
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
     * Сбросить статистику по режиму
     */
    fun resetModeStats(mode: String) {
        prefs.edit().apply {
            remove(keyModeGames(mode))
            remove(keyModeWins(mode))
            remove(keyModeStars(mode))
            remove(keyModeTime(mode))
            
            for (level in 1..3) {
                remove(keyGamesPlayed(mode, level))
                remove(keyGamesWon(mode, level))
                remove(keyBestTime(mode, level))
                remove(keyBestMoves(mode, level))
                remove(keyBestStars(mode, level))
                remove(keyTotalStars(mode, level))
            }
            apply()
        }
    }
}
