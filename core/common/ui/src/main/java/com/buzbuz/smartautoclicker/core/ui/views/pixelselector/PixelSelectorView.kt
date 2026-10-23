/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.ui.views.pixelselector

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toDrawable
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.PixelPositionComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ComponentsView
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor") // Not intended to be used from XML
class PixelSelectorView(
    context: Context,
    private val displayConfigManager: DisplayConfigManager,
    private val onSelectedPositionChanged: (PointF) -> Unit,
) : ComponentsView(context)  {

    /** The bitmap currently set to the view. */
    private var currentBitmap: Bitmap? = null
    
/** Whether rectangle selection is enabled. */
var rectangleSelectionEnabled: Boolean = false

/** Current drag-selection rectangle in view coordinates. */
private var dragStartX = 0f
private var dragStartY = 0f
private var dragEndX = 0f
private var dragEndY = 0f
private var isDragging = false

private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.GREEN
    style = Paint.Style.STROKE
    strokeWidth = 3f
}

/** Callback with the selected rectangle in view coordinates. */
var onSelectedAreaChanged: ((Rect) -> Unit)? = null
    
    /** Controls the display of the bitmap captured. */
    private lateinit var capture: CaptureComponent
    /** Controls the display the currently selected pixel. */
    private lateinit var position: PixelPositionComponent

    /** Get the attributes from the style file and initialize all components. */
    init {
        context.obtainPixelSelectorViewStyledAttributes().use { ta ->
            capture = CaptureComponent(context, ta.getCaptureComponentStyle(displayConfigManager), this)
            position = PixelPositionComponent(ta.getPixelPositionComponentStyle(displayConfigManager), this)
        }
    }

    override val viewComponents: List<ViewComponent> = listOf(capture, position)


    fun updateCapture(bitmap: Bitmap?) {
        if (bitmap == currentBitmap) return
        currentBitmap = bitmap

        capture.screenCapture = bitmap?.toDrawable(resources)
        invalidate()
    }

    fun updatePixelPosition(x: Float = position.pixelPosition.x, y: Float = position.pixelPosition.y) {
        if (position.pixelPosition.equals(x, y)) return
        position.pixelPosition.set(x.roundToInt().toFloat(), y.roundToInt().toFloat())
        invalidate()
    }

    
@SuppressLint("ClickableViewAccessibility")
override fun onTouchEvent(event: MotionEvent?): Boolean {
    event ?: return false

    if (!rectangleSelectionEnabled) {
        position.pixelPosition.set(
            event.x.roundToInt().toFloat(),
            event.y.roundToInt().toFloat()
        )
        onSelectedPositionChanged(
            PointF(position.pixelPosition.x, position.pixelPosition.y)
        )
        invalidate()
        return true
    }

    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            dragStartX = event.x
            dragStartY = event.y
            dragEndX = event.x
            dragEndY = event.y
            isDragging = true
            invalidate()
            return true
        }

        MotionEvent.ACTION_MOVE -> {
            if (isDragging) {
                dragEndX = event.x
                dragEndY = event.y
                invalidate()
            }
            return true
        }

        MotionEvent.ACTION_UP -> {
            if (isDragging) {
                dragEndX = event.x
                dragEndY = event.y
                isDragging = false

                val area = Rect(
                    minOf(dragStartX, dragEndX).roundToInt(),
                    minOf(dragStartY, dragEndY).roundToInt(),
                    maxOf(dragStartX, dragEndX).roundToInt(),
                    maxOf(dragStartY, dragEndY).roundToInt()
                )

                if (area.width() > 0 && area.height() > 0) {
                    onSelectedAreaChanged?.invoke(area)
                }

                invalidate()
            }
            return true
        }

        MotionEvent.ACTION_CANCEL -> {
            isDragging = false
            invalidate()
            return true
        }
    }

    return true
}


    private fun Context.obtainPixelSelectorViewStyledAttributes(): TypedArray =
        obtainStyledAttributes(null, R.styleable.PixelSelectorView, R.attr.pixelSelectorStyle, 0)
}
