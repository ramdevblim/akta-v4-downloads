package com.akta.luraplayercontrols.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import com.akta.luraplayercontrols.R

class LuraControlsSharedPreferencesUtil private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "LuraControlsSharedPref"

        private const val KEY_CAPTIONS_FONT = "captions_font"
        private const val KEY_CAPTIONS_FONT_SIZE = "captions_font_size"
        private const val KEY_CAPTIONS_TEXT_COLOR = "captions_text_color"
        private const val KEY_CAPTIONS_TEXT_OPACITY = "captions_text_opacity"
        private const val KEY_CAPTIONS_BACKGROUND_COLOR = "captions_background_color"
        private const val KEY_CAPTIONS_BACKGROUND_OPACITY = "captions_background_opacity"
        private const val KEY_CAPTIONS_HIGHLIGHT_COLOR = "captions_highlight_color"
        private const val KEY_CAPTIONS_HIGHLIGHT_OPACITY = "captions_highlight_opacity"
        private const val KEY_CAPTIONS_CAPITALIZE = "captions_capitalize"

        @Volatile
        private var INSTANCE: LuraControlsSharedPreferencesUtil? = null

        @Synchronized
        fun getInstance(context: Context): LuraControlsSharedPreferencesUtil {
            return INSTANCE ?: synchronized(this) {
                val instance = LuraControlsSharedPreferencesUtil(context)
                INSTANCE = instance
                instance
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    var captionsFont: Int
        @FontRes
        get() = sharedPreferences.getInt(KEY_CAPTIONS_FONT, R.font.inter)
        set(value) {
            editor.putInt(KEY_CAPTIONS_FONT, value)
            editor.apply()
        }

    var captionsFontSize: Float
        get() = sharedPreferences.getFloat(KEY_CAPTIONS_FONT_SIZE, 12f)
        set(value) {
            editor.putFloat(KEY_CAPTIONS_FONT_SIZE, value)
            editor.apply()
        }

    var captionsTextColor: Int
        @ColorRes
        get() = sharedPreferences.getInt(KEY_CAPTIONS_TEXT_COLOR, R.color.white)
        set(value) {
            editor.putInt(KEY_CAPTIONS_TEXT_COLOR, value)
            editor.apply()
        }

    var captionsTextOpacity: Int
        get() = sharedPreferences.getInt(
            KEY_CAPTIONS_TEXT_OPACITY,
            100
        )
        set(value) {
            editor.putInt(KEY_CAPTIONS_TEXT_OPACITY, value)
            editor.apply()
        }

    var captionsBackgroundColor: Int
        @ColorRes
        get() = sharedPreferences.getInt(KEY_CAPTIONS_BACKGROUND_COLOR, R.color.black)
        set(value) {
            editor.putInt(KEY_CAPTIONS_BACKGROUND_COLOR, value)
            editor.apply()
        }

    var captionsBackgroundOpacity: Int
        get() = sharedPreferences.getInt(
            KEY_CAPTIONS_BACKGROUND_OPACITY,
            100
        )
        set(value) {
            editor.putInt(KEY_CAPTIONS_BACKGROUND_OPACITY, value)
            editor.apply()
        }

    var captionsHighlightColor: Int
        @ColorRes
        get() = sharedPreferences.getInt(KEY_CAPTIONS_HIGHLIGHT_COLOR, R.color.black)
        set(value) {
            editor.putInt(KEY_CAPTIONS_HIGHLIGHT_COLOR, value)
            editor.apply()
        }

    var captionsHighlightOpacity: Int
        get() = sharedPreferences.getInt(
            KEY_CAPTIONS_HIGHLIGHT_OPACITY,
            0
        )
        set(value) {
            editor.putInt(KEY_CAPTIONS_HIGHLIGHT_OPACITY, value)
            editor.apply()
        }

    var captionsCapitalize: Boolean
        get() = sharedPreferences.getBoolean(KEY_CAPTIONS_CAPITALIZE, false)
        set(value) {
            editor.putBoolean(KEY_CAPTIONS_CAPITALIZE, value)
            editor.apply()
        }

    fun getOpacityFromPercent(percent: Int): Int {
        return 255 * percent / 100;
    }
}