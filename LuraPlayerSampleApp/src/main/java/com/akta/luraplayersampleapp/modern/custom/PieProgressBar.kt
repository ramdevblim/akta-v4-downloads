package com.akta.luraplayersampleapp.modern.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.akta.luraplayersampleapp.R

class PieProgressBar(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var progress = 0
    private val maxProgress = 100

    private val progressPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.white)
    }

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, com.akta.luraplayercontrols.R.color.transparent)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2 * 0.8).toFloat()

        val oval = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Calculate the angle based on the progress
        val angle = 360 * (progress.toFloat() / maxProgress)

        // Draw progress arc
        canvas.drawArc(oval, -90f, angle, true, progressPaint)
    }

    fun setProgress(progress: Int) {
        this.progress = progress.coerceIn(0, maxProgress)
        invalidate()
    }
}