
/*
 * Copyright (C) 2026 Kevin Buzeau
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.buzbuz.smartautoclicker.core.ui.views.pixelselector

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.view.MotionEvent
import androidx.core.content.res.use
import androidx.core.graphics.drawable.toDrawable
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.PixelPositionComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ComponentsView
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class PixelSelectorView(
    context: Context,
    private val displayConfigManager: DisplayConfigManager,
    private val onSelectedPositionChanged: (PointF) -> Unit,
) : ComponentsView(context) {

    private var currentBitmap: Bitmap? = null

    var rectangleSelectionEnabled: Boolean = false
    var onSelectedAreaChanged: ((Rect) -> Unit)? = null

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

    private lateinit var capture: CaptureComponent
    private lateinit var position: PixelPositionComponent

    init {
        context.obtainPixelSelectorViewStyledAttributes().use { ta ->
            capture = CaptureComponent(
                context,
                ta.getCaptureComponentStyle(displayConfigManager),
                this,
            )
            position = PixelPositionComponent(
                ta.getPixelPositionComponentStyle(displayConfigManager),
                this,
            )
        }
    }

    override val viewComponents: List<ViewComponent> = listOf(capture, position)

    fun updateCapture(bitmap: Bitmap?) {
        if (bitmap == currentBitmap) return
        currentBitmap = bitmap
        capture.screenCapture = bitmap?.toDrawable(resources)
        invalidate()
    }

    fun updatePixelPosition(
        x: Float = position.pixelPosition.x,
        y: Float = position.pixelPosition.y,
    ) {
        if (position.pixelPosition.equals(x, y)) return
        position.pixelPosition.set(x.roundToInt().toFloat(), y.roundToInt().toFloat())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (rectangleSelectionEnabled &&
            (isDragging || dragStartX != dragEndX || dragStartY != dragEndY)
        ) {
            canvas.drawRect(
                min(dragStartX, dragEndX),
                min(dragStartY, dragEndY),
                max(dragStartX, dragEndX),
                max(dragStartY, dragEndY),
                selectionPaint,
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        if (!rectangleSelectionEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP -> {
                    position.pixelPosition.set(
                        event.x.roundToInt().toFloat(),
                        event.y.roundToInt().toFloat(),
                    )
                    onSelectedPositionChanged(
                        PointF(position.pixelPosition.x, position.pixelPosition.y),
                    )
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> return true
            }
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
                        min(dragStartX, dragEndX).roundToInt(),
                        min(dragStartY, dragEndY).roundToInt(),
                        max(dragStartX, dragEndX).roundToInt(),
                        max(dragStartY, dragEndY).roundToInt(),
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
        obtainStyledAttributes(
            null,
            R.styleable.PixelSelectorView,
            R.attr.pixelSelectorStyle,
            0,
        )
}
