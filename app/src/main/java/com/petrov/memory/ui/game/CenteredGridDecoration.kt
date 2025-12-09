package com.petrov.memory.ui.game

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration для центрирования сетки карточек и добавления равномерных отступов
 */
class CenteredGridDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
        val spanCount = layoutManager.spanCount
        val position = parent.getChildAdapterPosition(view)
        
        if (position < 0) return
        
        val column = position % spanCount
        
        // Равномерные отступы между карточками
        outRect.left = spacing / 2
        outRect.right = spacing / 2
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
    }
}
