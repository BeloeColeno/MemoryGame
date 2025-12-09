package com.petrov.memory.ui.game

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration для добавления фиксированных отступов между карточками
 */
class CenteredGridDecoration(
    private val spacing: Int,
    private val spanCount: Int,
    private val totalItems: Int
) : RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position < 0) return
        
        // Равномерные минимальные зазоры между карточками
        outRect.left = spacing / 2
        outRect.right = spacing / 2
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
    }
}
