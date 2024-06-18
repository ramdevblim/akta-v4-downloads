package com.akta.luraplayercontrols.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

class LuraImageButton : AppCompatImageButton {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        setPaddingRelative(16, 16, 16, 16)
    }

    override fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            imageAlpha = if (enabled) 0xFF else 0x66
        }
        super.setEnabled(enabled)
    }
}