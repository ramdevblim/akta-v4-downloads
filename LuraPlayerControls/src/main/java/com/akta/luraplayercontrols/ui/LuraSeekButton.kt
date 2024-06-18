package com.akta.luraplayercontrols.ui

import android.content.Context
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import android.view.View

class LuraSeekButton : View {
    private val transition = background as TransitionDrawable
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun startTransition() {
        transition.startTransition(300)
    }

    fun reverseTransaction() {
        transition.reverseTransition(300)
    }
}