package com.petrov.memory.ui.game

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration для центрирования сетки карточек и добавления равномерных отступов
 */
class CenteredGridDecoration(
    private val spacing: Int,
    private val spanCount: Int,
    private val totalItems: Int
) : RecyclerView.ItemDecoration() {
    
    private var horizontalPadding = 0
    private var verticalPadding = 0
    private var isInitialized = false
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position < 0) return
        
        // Инициализируем padding для центрирования только один раз
        if (!isInitialized && parent.childCount > 0) {
            calculateCenterPadding(parent)
            isInitialized = true
        }
        
        // НЕ добавляем offsets - padding идет из item_card.xml
        // Оставляем пустым, чтобы не дублировать отступы
    }
    
    /**
     * Вычисляем padding для центрирования сетки
     */
    private fun calculateCenterPadding(parent: RecyclerView) {
        if (parent.childCount == 0) return
        
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
        
        // Получаем размер первой карточки
        val firstChild = parent.getChildAt(0) ?: return
        val cardSize = firstChild.width
        
        if (cardSize <= 0) return
        
        // Вычисляем количество строк
        val rows = (totalItems + spanCount - 1) / spanCount
        
        // Вычисляем общие размеры сетки
        val totalGridWidth = cardSize * spanCount + spacing * (spanCount - 1)
        val totalGridHeight = cardSize * rows + spacing * (rows - 1)
        
        // Вычисляем свободное пространство
        val parentWidth = parent.width
        val parentHeight = parent.height
        
        horizontalPadding = maxOf(0, (parentWidth - totalGridWidth) / 2)
        verticalPadding = maxOf(0, (parentHeight - totalGridHeight) / 2)
        
        // Устанавливаем padding на RecyclerView для центрирования
        parent.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        parent.clipToPadding = false
        
        android.util.Log.d("CenteredGrid", "Grid centered: hPad=$horizontalPadding, vPad=$verticalPadding, gridSize=${totalGridWidth}x${totalGridHeight}, parentSize=${parentWidth}x${parentHeight}")
    }
}
