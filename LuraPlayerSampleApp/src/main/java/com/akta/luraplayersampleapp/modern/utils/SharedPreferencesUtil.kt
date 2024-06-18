package com.akta.luraplayersampleapp.modern.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesUtil private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "ModernDemoApp"

        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_MAX_STORAGE = "max_storage"
        private const val KEY_PARALLEL_DOWNLOADS = "parallel_downloads"
        private const val KEY_DOWNLOAD_QUALITY = "download_quality"

        @Volatile
        private var INSTANCE: SharedPreferencesUtil? = null

        fun getInstance(context: Context): SharedPreferencesUtil {
            return INSTANCE ?: synchronized(this) {
                val instance = SharedPreferencesUtil(context)
                INSTANCE = instance
                instance
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    var wifiOnly: Boolean
        get() = sharedPreferences.getBoolean(KEY_WIFI_ONLY, true)
        set(value) {
            editor.putBoolean(KEY_WIFI_ONLY, value)
            editor.apply()
        }

    var maxStorage: Long
        get() = sharedPreferences.getLong(KEY_MAX_STORAGE, 2500000000L)
        set(value) {
            editor.putLong(KEY_MAX_STORAGE, value)
            editor.apply()
        }

    var maxParallelDownloads: Int
        get() = sharedPreferences.getInt(KEY_PARALLEL_DOWNLOADS, 3)
        set(value) {
            editor.putInt(KEY_PARALLEL_DOWNLOADS, value)
            editor.apply()
        }

    var videoQuality: String
        get() = sharedPreferences.getString(KEY_DOWNLOAD_QUALITY, "SD") ?: "SD"
        set(value) {
            editor.putString(KEY_DOWNLOAD_QUALITY, value)
            editor.apply()
        }
}