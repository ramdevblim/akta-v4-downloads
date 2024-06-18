package com.akta.luraplayercontrols.ui

import android.R.attr.radius
import android.R.attr.topLeftRadius
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


class RoundedCornerImageView : AppCompatImageView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val cornerRadius = 8F

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val rect = RectF(0f, 0f, this.width.toFloat(), this.height.toFloat())
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)

        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}