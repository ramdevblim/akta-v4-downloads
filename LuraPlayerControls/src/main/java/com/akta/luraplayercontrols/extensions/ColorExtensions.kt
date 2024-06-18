package com.akta.luraplayercontrols.extensions

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat

fun Context.convertColor(colorRes: Int, opacity: Int = -1): Int {
    val color = try {
        ContextCompat.getColor(this, colorRes)
    } catch (e: Exception) {
        colorRes
    }
    return if (opacity < 0) {
        color
    } else {
        Color.argb(
            opacity,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}