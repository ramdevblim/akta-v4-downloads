package com.akta.luraplayercontrols.extensions

import android.text.format.DateUtils

fun Long.toSecondsString() = DateUtils.formatElapsedTime(this) ?: ""

fun Long.toSeconds() = this.div(1000.0)
fun Double.toMs() = this.times(1000).toLong()
