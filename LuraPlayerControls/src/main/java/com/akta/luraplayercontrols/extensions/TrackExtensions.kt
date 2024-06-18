package com.akta.luraplayercontrols.extensions

import android.content.Context
import com.akta.luraplayer.api.models.LuraTrack
import com.akta.luraplayercontrols.R
import java.util.Locale

fun LuraTrack.Audio.toBitrateName(context: Context? = null): String {
    return try {
        if (bitrate == null) {
            toLanguageName(context)
        } else {
            (toLanguageName(context) + " (" + context?.getString(
                R.string.luraBitrate,
                bitrate?.toFloat()?.div(1000)
            ) + ")")
        }
    } catch (_: Exception) {
        ""
    }
}

fun LuraTrack.Audio.toLanguageName(context: Context? = null): String {
    return try {
        if (language == null) return context?.getString(R.string.luraTrack, index) ?: "Track $index"
        val code = if (language?.contains("-") == true) {
            language?.split("-")?.getOrNull(0) ?: language ?: ""
        } else language ?: ""
        Locale(code).displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    } catch (_: Exception) {
        context?.getString(R.string.luraTrack, index) ?: "Track $index"
    }
}

fun LuraTrack.Text.toLanguageName(context: Context? = null): String {
    return try {
        if (isSelectedTrackOff) return context?.getString(R.string.luraOff) ?: "Off"
        if (language == null) return context?.getString(R.string.luraTrack, index) ?: "Track $index"
        val code =
            if (language?.contains("-") == true) {
                language?.split("-")?.getOrNull(0) ?: language ?: ""
            } else language ?: ""
        Locale(code).displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    } catch (_: Exception) {
        context?.getString(R.string.luraTrack, index) ?: "Track $index"
    }
}


fun LuraTrack.Video.toBitrateName(context: Context? = null): String {
    return try {
        if (index == -1) {
            if (active) {
                context?.getString(R.string.luraAutoBitrate, bitrate?.toFloat()?.div(1000))
                    ?: "Auto"
            } else {
                context?.getString(R.string.luraAuto) ?: "Auto"
            }
        } else {
            if (bitrate != null) {
                context?.getString(R.string.luraBitrate, bitrate?.toFloat()?.div(1000))
                    ?: "%.0f kbps".format(bitrate?.toFloat()?.div(1000))
            } else
                ""
        }
    } catch (_: Exception) {
        ""
    }
}