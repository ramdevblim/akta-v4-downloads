package com.akta.luraplayersampleapp.modern.custom

import android.annotation.SuppressLint
import android.app.Activity
import android.widget.FrameLayout
import android.widget.TextView
import com.akta.luraplayer.api.offline.LuraOfflineVideo
import com.akta.luraplayersampleapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date

class ConfigurationSheet(
    activity: Activity,
    private val offlineVideo: LuraOfflineVideo
) : BottomSheetDialog(activity) {

    private val assetID: TextView
    private val progress: TextView
    private val totalBytes: TextView
    private val currentBytes: TextView
    private val state: TextView
    private val startTime: TextView
    private val updateTime: TextView
    private val licenceTotalPlaybackDuration: TextView
    private val licenceExpirationDuration: TextView

    init {
        setContentView(R.layout.modern_configuration_sheet)
        val view = this.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        view?.background = null

        assetID = findViewById(R.id.asset_id)!!
        progress = findViewById(R.id.progress)!!
        totalBytes = findViewById(R.id.total_bytes)!!
        currentBytes = findViewById(R.id.current_bytes)!!
        state = findViewById(R.id.state)!!
        startTime = findViewById(R.id.start_time)!!
        updateTime = findViewById(R.id.update_time)!!
        licenceTotalPlaybackDuration = findViewById(R.id.licence_total_playback_duration)!!
        licenceExpirationDuration = findViewById(R.id.licence_expiration_duration)!!

        assetID.text =
            context.resources.getString(R.string.configuration_asset_id, offlineVideo.assetId)
        progress.text =
            context.resources.getString(
                R.string.configuration_progress,
                offlineVideo.progress.toString()
            )
        totalBytes.text =
            context.resources.getString(
                R.string.configuration_total_bytes,
                offlineVideo.total.asHumanReadableBytes()
            )
        currentBytes.text =
            context.resources.getString(
                R.string.configuration_current_bytes,
                offlineVideo.bytes.asHumanReadableBytes()
            )
        state.text =
            context.resources.getString(R.string.configuration_state, offlineVideo.state)
        startTime.text =
            context.resources.getString(
                R.string.configuration_start_time,
                timestampToDateTime(offlineVideo.startTimeMs)
            )
        updateTime.text =
            context.resources.getString(
                R.string.configuration_update_time,
                timestampToDateTime(offlineVideo.updateTimeMs)
            )

        licenceTotalPlaybackDuration.text =
            context.resources.getString(
                R.string.configuration_licence_total_playback_duration,
                offlineVideo.license?.totalPlaybackDuration.toHourMinuteSecond()
            )
        licenceExpirationDuration.text =
            context.resources.getString(
                R.string.configuration_licence_expiration_duration,
                offlineVideo.license?.licenseExpirationDate.toHourMinuteSecond()
            )


        findViewById<FrameLayout>(R.id.option_close)?.setOnClickListener {
            dismiss()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun timestampToDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = Date(timestamp)
        return dateFormat.format(date)
    }
}