package com.akta.luraplayercontrols.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.akta.luraplayercontrols.R

class LuraSplashView : ConstraintLayout {
    private val splashPoster: ImageView
    internal val playReplayButton: LuraImageButton

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        View.inflate(context, R.layout.lura_splash_view, this)
        setOnClickListener {}
        splashPoster = findViewById(R.id.lura_splash_poster_view)
        playReplayButton = findViewById(R.id.lura_play_replay_button)
    }

    fun setSplash() {
        playReplayButton.apply {
            contentDescription = resources.getString(R.string.play)
            setImageResource(R.drawable.ic_play)
        }
        visibility = VISIBLE
    }

    fun reset() {
        visibility = GONE
        setPoster(null)
        playReplayButton.apply {
            contentDescription = resources.getString(R.string.play)
            setImageResource(R.drawable.ic_play)
        }
    }

    fun setPoster(bitmap: Bitmap?) {
        splashPoster.setImageBitmap(bitmap)
    }
}