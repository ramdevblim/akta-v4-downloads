package com.akta.luraplayercontrols.ui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

import com.akta.luraplayer.api.event.LuraEventData
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.extensions.toSecondsString


class LuraTimeView : ConstraintLayout {

    private val circleView: View
    private val timeView: TextView

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        View.inflate(context, R.layout.lura_time_view, this)
        circleView = findViewById(R.id.luraInLive)
        timeView = findViewById(R.id.luraTimeTextView)
    }

    internal fun setTime(event: LuraEventData.TimeUpdate, isLive: Boolean, isDelayedLive: Boolean) {
        if (isLive) {
            circleView.visibility = View.VISIBLE
            isEnabled = isDelayedLive
            timeView.text = context.getString(R.string.luraLive)
        } else {
            circleView.visibility = View.GONE
            isEnabled = false
            val duration = event.durationMs
            val posStr = (event.contentTimeMs / 1000).toSecondsString()
            val durStr = (duration / 1000L).toSecondsString()
            timeView.text = resources.getString(R.string.lura_video_time, posStr, durStr)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        val background: Drawable = circleView.background
        val color = if (enabled) ContextCompat.getColor(context,R.color.white30)
        else ContextCompat.getColor(context,R.color.live)
        when (background) {
            is ShapeDrawable -> background.paint.color = color
            is GradientDrawable -> background.setColor(color)
            is ColorDrawable -> background.color = color
        }

        circleView.background = background
    }

    fun setTime(text: String) {
        timeView.text = text
    }
}