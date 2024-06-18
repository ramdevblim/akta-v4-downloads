package com.akta.luraplayersampleapp.modern.custom

import android.content.Context
import com.akta.luraplayer.api.logger.LuraLog
import com.akta.luraplayersampleapp.BuildConfig
import com.akta.luraplayersampleapp.modern.data.Assets
import com.akta.luraplayersampleapp.modern.data.Video
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private var dashboardCachedJsonData: String? = null
private var verticalCachedJsonData: String? = null

fun Context.getDashboardAssets(): Map<String, List<Video>> {
    if (dashboardCachedJsonData == null) {
        dashboardCachedJsonData = try {
            this.assets.open("videos.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            LuraLog.e("AssetsReader", e.message ?: "videos.json read error", e)
            return emptyMap()
        }
    }

    return try {
        val allAssets: MutableMap<String, List<Video>> = mutableMapOf()
        val jsonString = dashboardCachedJsonData ?: return emptyMap()

        json.decodeFromString<List<Assets>>(jsonString).forEach {
            if (!BuildConfig.DEBUG) {
                if (!it.debugOnly) {
                    val videoList: MutableList<Video> = mutableListOf()
                    it.configs.forEach { video ->
                        if (!video.debugOnly) {
                            videoList.add(video)
                        }
                    }
                    allAssets[it.groupTitle] = videoList
                }
            } else {
                allAssets[it.groupTitle] = it.configs
            }
        }
        allAssets
    } catch (e: Exception) {
        LuraLog.e("AssetsReader", e.message ?: "", e)
        emptyMap()
    }
}

fun Context.getVerticalAssets(): List<Video> {
    if (verticalCachedJsonData == null) {
        verticalCachedJsonData = try {
            this.assets.open("vertical-videos.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            LuraLog.e("AssetsReader", e.message ?: "vertical-videos.json read error", e)
            return emptyList()
        }
    }

    return try {
        val videoList: MutableList<Video> = mutableListOf()
        val jsonString = verticalCachedJsonData ?: return emptyList()

        json.decodeFromString<List<Video>>(jsonString).forEach {
            if (!BuildConfig.DEBUG) {
                if (!it.debugOnly) {
                    videoList.add(it)
                }
            } else {
                videoList.add(it)
            }
        }
        videoList
    } catch (e: Exception) {
        LuraLog.e("AssetsReader", e.message ?: "", e)
        emptyList()
    }
}