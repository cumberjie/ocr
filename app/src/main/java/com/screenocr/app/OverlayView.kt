package com.screenocr.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context) : View(context) {

    var onRegionSelected: ((Rect) -> Unit)? = null

    private val overlayPaint = Paint().apply {
        color = Color.argb(128, 0, 0, 0) // 50% transparent black
        style = Paint.Style.FILL
    }

    private val selectionPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val selectionFillPaint = Paint().apply {
        color = Color.argb(50, 255, 255, 255) // Light white fill
        style = Paint.Style.FILL
    }

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isSelecting = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw semi-transparent overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Draw selection rectangle if selecting
        if (isSelecting) {
            val left = min(startX, currentX)
            val top = min(startY, currentY)
            val right = max(startX, currentX)
            val bottom = max(startY, currentY)

            // Clear the selected area (make it transparent)
            canvas.save()
            canvas.clipRect(left, top, right, bottom)
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            canvas.restore()

            // Draw selection fill
            canvas.drawRect(left, top, right, bottom, selectionFillPaint)

            // Draw selection border
            canvas.drawRect(left, top, right, bottom, selectionPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                currentX = event.x
                currentY = event.y
                isSelecting = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isSelecting = false

                val left = min(startX, currentX).toInt()
                val top = min(startY, currentY).toInt()
                val right = max(startX, currentX).toInt()
                val bottom = max(startY, currentY).toInt()

                // Only trigger callback if selection is large enough
                if (right - left > 10 && bottom - top > 10) {
                    val rect = Rect(left, top, right, bottom)
                    onRegionSelected?.invoke(rect)
                }

                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
