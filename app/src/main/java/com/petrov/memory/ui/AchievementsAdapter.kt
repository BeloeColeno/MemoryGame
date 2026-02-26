package com.petrov.memory.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.petrov.memory.R
import com.petrov.memory.data.Achievement

class AchievementsAdapter(private val achievements: List<Achievement>) :
    RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.achievementIcon)
        val title: TextView = view.findViewById(R.id.achievementTitle)
        val description: TextView = view.findViewById(R.id.achievementDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        
        holder.icon.setImageResource(achievement.iconResId)
        holder.title.text = achievement.title
        holder.description.text = achievement.description
        
        if (!achievement.isUnlocked) {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            holder.icon.colorFilter = filter
            holder.icon.alpha = 0.3f
            holder.title.alpha = 0.5f
            holder.description.alpha = 0.5f
        } else {
            holder.icon.colorFilter = null
            holder.icon.alpha = 1f
            holder.title.alpha = 1f
            holder.description.alpha = 1f
        }
    }

    override fun getItemCount() = achievements.size
}
