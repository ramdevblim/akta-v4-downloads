package com.akta.luraplayercontrols.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color.YELLOW
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap

import com.akta.luraplayer.api.event.LuraEventData
import com.akta.luraplayercontrols.R


class LuraSeekBarWithMarks : AppCompatSeekBar {
    private var mDotsPositions: DoubleArray? = null
    private var seekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    private var mDotBitmap: Bitmap = Bitmap.createBitmap(
        resources.getDimension(R.dimen.ad_mark_width).toInt(),
        resources.getDimension(R.dimen.seekbar_height).toInt(),
        Bitmap.Config.ARGB_8888
    ).apply {
        eraseColor(YELLOW)
    }

    init {
        val padding = (5 * Resources.getSystem().displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)

        keyProgressIncrement = 30000
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        val td = ResourcesCompat.getDrawable(resources, R.drawable.seekbar_thumb, context.theme)
        thumb = if (gainFocus) {
            val bitmap = (td as VectorDrawable).toBitmap()
            BitmapDrawable(
                resources,
                Bitmap.createScaledBitmap(
                    bitmap,
                    bitmap.width,
                    bitmap.height.times(3),
                    true
                )
            )
        } else td

        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    fun setDots(dots: DoubleArray) {
        mDotsPositions = dots
        invalidate()
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener?) {
        this.seekBarChangeListener = l
        super.setOnSeekBarChangeListener(l)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isFocused && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            seekBarChangeListener?.onStopTrackingTouch(this)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isFocused && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            seekBarChangeListener?.onStartTrackingTouch(this)
        }

        return super.onKeyDown(keyCode, event)
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (max > 0 && (mDotsPositions?.isNotEmpty() == true)) {
            val width = width - paddingStart - paddingEnd
            val step: Double = width.toDouble() / (100.0)
            for (position in mDotsPositions!!) {
                canvas.drawBitmap(
                    mDotBitmap,
                    paddingStart + when (position) {
                        0.0 -> 0.0F
                        else -> (position * step).toFloat() - mDotBitmap.width.toFloat()
                    },
                    paddingTop.toFloat(),
                    null
                )
            }
        }
    }

    fun setPosition(event: LuraEventData.TimeUpdate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            min = 0
        }
        max = event.durationMs.toInt()
        keyProgressIncrement = 2500
        progress = event.contentTimeMs.toInt()
    }

    fun setBufferedProgress(buffered: Long) {
        secondaryProgress = buffered.toInt()
    }

}