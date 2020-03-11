package com.android.example.cameraxbasic.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.tarento.markreader.utils.GraphicOverlay

class RecGraphic(overlay: GraphicOverlay) :
    GraphicOverlay.Graphic(overlay) {

    private var rectPaint = Paint().apply {
        color = TEXT_COLOR
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
    }

    private var barcodePaint = Paint().apply {
        color = TEXT_COLOR
        textSize = TEXT_SIZE
    }

    /**
     * Draws the barcode block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        // Draws the bounding box around the BarcodeBlock.
//        val rect = RectF(barcode.boundingBox)
//        rect.left = translateX(rect.left)
//        rect.top = translateY(rect.top)
//        rect.right = translateX(rect.right)
//        rect.bottom = translateY(rect.bottom)
        val rect = RectF()
        rect.left = 10f
        rect.top = 10f
        rect.right = 100f
        rect.bottom = 100f
        canvas.drawRect(rect, rectPaint)

//        // Renders the barcode at the bottom of the box.
//        barcode.rawValue?.let { value ->
//            canvas.drawText(value, rect.left, rect.bottom, barcodePaint)
//        }
    }

    companion object {
        private const val TEXT_COLOR = Color.RED
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
    }
}