package com.akta.luraplayercontrols.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.akta.luraplayercontrols.R


class LuraAdSkipButton : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var durationText: TextView
    private var contentPreview: RoundedCornerImageView

    var text: String = ""
        set(value) {
            field = value
            durationText.text = value
        }

    var preview: Bitmap? = null
        set(value) {
            field = value
            contentPreview.setImageBitmap(value)
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.lura_ad_skip_button_view, this, true)

        durationText = findViewById(R.id.duration_text)
        contentPreview = findViewById(R.id.content_preview)
    }
}