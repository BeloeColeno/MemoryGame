package com.petrov.memory.ui.game

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration для центрирования сетки карточек на экране
 * с минимальными зазорами между карточками
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
        val itemCount = state.itemCount
        val position = parent.getChildAdapterPosition(view)
        
        if (position < 0) return
        
        val column = position % spanCount
        val row = position / spanCount
        val totalRows = (itemCount + spanCount - 1) / spanCount
        
        // Минимальные зазоры между карточками (половина с каждой стороны)
        outRect.left = spacing / 2
        outRect.right = spacing / 2
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
        
        // Добавляем padding для центрирования сетки
        // Вычисляем размер всей сетки
        val firstChild = parent.getChildAt(0) ?: return
        val childWidth = firstChild.width
        val childHeight = firstChild.height
        
        if (childWidth > 0 && childHeight > 0) {
            val totalGridWidth = childWidth * spanCount + spacing * (spanCount - 1)
            val totalGridHeight = childHeight * totalRows + spacing * (totalRows - 1)
            
            val parentWidth = parent.width
            val parentHeight = parent.height
            
            val horizontalPadding = maxOf(0, (parentWidth - totalGridWidth) / 2)
            val verticalPadding = maxOf(0, (parentHeight - totalGridHeight) / 2)
            
            // Добавляем центрирующий padding к первой/последней колонке и строке
            if (column == 0) {
                outRect.left += horizontalPadding
            }
            if (column == spanCount - 1) {
                outRect.right += horizontalPadding
            }
            if (row == 0) {
                outRect.top += verticalPadding
            }
            if (row == totalRows - 1) {
                outRect.bottom += verticalPadding
            }
        }
    }
}
