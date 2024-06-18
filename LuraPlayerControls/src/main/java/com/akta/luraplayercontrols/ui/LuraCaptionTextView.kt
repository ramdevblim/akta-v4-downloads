package com.akta.luraplayercontrols.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.akta.luraplayercontrols.extensions.convertColor
import kotlin.math.ceil

class LuraCaptionTextView : AppCompatTextView {

    private var isCapitalize: Boolean = false
    private var autoSize: Boolean = false
    private val autoSizeSentence: String =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (autoSize)
            setCaptionFontSize(-1f)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        val finalText = if (isCapitalize)
            text.toString().uppercase()
        else
            text.toString()
        super.setText(finalText, type)
    }

    fun setCaptionFont(font: Int) {
        typeface = ResourcesCompat.getFont(context, font)
    }

    fun setCaptionFontSize(size: Float) {
        autoSize = size < 0f
        if (autoSize) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, calculateAutoFontSize())
        } else {
            textSize = size
        }
    }

    fun setCaptionTextColor(textColor: Int, opacity: Int = -1) {
        val color = context.convertColor(textColor, opacity)
        setTextColor(color)
    }

    fun setCaptionTextOpacity(opacity: Int) {
        setCaptionTextColor(currentTextColor, opacity)
    }

    fun setCaptionBackgroundColor(backgroundColor: Int, opacity: Int = -1) {
        val color = context.convertColor(backgroundColor, opacity)
        setBackgroundColor(color)
    }

    fun setCaptionBackgroundOpacity(opacity: Int) {
        val color = (background as ColorDrawable).color
        setCaptionBackgroundColor(color, opacity)
    }

    fun setCaptionHighlightColor(highlightColor: Int, opacity: Int = -1) {
        val color = context.convertColor(highlightColor, opacity)
        setShadowLayer(15f, 0f, 0f, color)
    }

    fun setCaptionHighlightOpacity(opacity: Int) {
        setCaptionHighlightColor(shadowColor, opacity)
    }

    fun setCaptionCapitalize(capitalize: Boolean = false) {
        isCapitalize = capitalize
        text = if (capitalize) text.toString().uppercase() else text
    }

    private fun calculateAutoFontSize(): Float {
        val availableWidth =
            resources.displayMetrics.widthPixels - paddingLeft - paddingRight
        var low = 0f
        var high = 100f
        var bestSize = high
        while (low <= high) {
            val currentSize = (low + high) / 2
            paint.textSize = currentSize
            val textWidth =
                paint.measureText(autoSizeSentence)
            val numLines = ceil(textWidth / availableWidth)

            if (numLines <= 1) {
                bestSize = currentSize
                low = currentSize + 1
            } else {
                high = currentSize - 1
            }
        }
        return bestSize
    }
}