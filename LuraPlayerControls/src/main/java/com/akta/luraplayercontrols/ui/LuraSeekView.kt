package com.akta.luraplayercontrols.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.akta.luraplayercontrols.R


class LuraSeekView(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {
    private val titleView: TextView
    private val seekImage: ImageView


    init {
        View.inflate(context, R.layout.lura_seek_view, this)
        titleView = findViewById(R.id.seekTitleView)
        seekImage = findViewById(R.id.seekButtonView)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LuraSeekView,
            0, 0
        ).apply {
            try {
                seekImage.setImageResource(
                    getResourceId(R.styleable.LuraSeekView_luraSrc, R.drawable.seek_forward)
                )
                titleView.text = getString(R.styleable.LuraSeekView_luraSeekTime)
            } finally {
                recycle()
            }
        }
    }

    fun setSeekCount(timeToSeekSec: Long) {
        titleView.text = "$timeToSeekSec seconds"
    }

}