package com.akta.luraplayersampleapp.modern

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.scheduler.Requirements
import com.akta.luraplayer.api.LuraOfflineEventListener
import com.akta.luraplayer.api.logger.LuraLog
import com.akta.luraplayer.api.offline.LuraDownloadService
import com.akta.luraplayer.api.offline.LuraOfflineVideo
import com.akta.luraplayer.api.offline.event.LuraOfflineEvent
import com.akta.luraplayer.api.offline.event.LuraOfflineEventData
import com.akta.luraplayer.api.offline.event.LuraOfflineEventType
import com.akta.luraplayersampleapp.R

@SuppressLint("UnsafeOptInUsageError")
class DemoAppDownloadService : LuraDownloadService() {
    private val listener: LuraOfflineEventListener = LuraOfflineEventListener {
        onDownloadChanged(it)
    }

    override fun onCreate() {
        super.onCreate()
        offlineManager.addListener(listener)
    }

    private fun onDownloadChanged(event: LuraOfflineEvent) {
        LuraLog.d("LuraDownloadService", "$event")
        when (val data = event.data) {
            is LuraOfflineEventData.Multiple -> {
                val videos = data.videos
                videos.forEach { setNotificationForDownload(event, it) }
            }

            is LuraOfflineEventData.Error -> setNotificationForDownload(event, data.video)
            is LuraOfflineEventData.Single -> setNotificationForDownload(event, data.video)
            is LuraOfflineEventData.Warning -> {}
        }
    }

    override fun getForegroundNotification(
        downloads: List<Download>, notMetRequirements: @Requirements.RequirementFlags Int,
    ): Notification {
        val message = downloads.filter { it.state == Download.STATE_DOWNLOADING }
            .joinToString(separator = "\n")
            {
                "Asset ID: ${it.request.id} (${
                    String.format(
                        "%.1f",
                        it.percentDownloaded
                    )
                }%)"
            }

        return notificationHelper.buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            null,
            message,
            downloads,
            notMetRequirements
        )
    }

    private fun setNotificationForDownload(
        event: LuraOfflineEvent,
        video: LuraOfflineVideo
    ) {
        val notification: Notification? = when (event.type) {
            LuraOfflineEventType.COMPLETED -> notificationHelper.buildDownloadCompletedNotification(
                this,
                R.drawable.ic_download_done,
                null,
                video.assetId
            )

            LuraOfflineEventType.QUEUED -> NotificationCompat.Builder(this, "download_channel")
                .setContentIntent(null)
                .setContentTitle("QUEUED")
                .setSmallIcon(R.drawable.ic_queue)
                .setStyle(NotificationCompat.BigTextStyle().bigText(video.assetId))
                .build()

            LuraOfflineEventType.PAUSED -> NotificationCompat.Builder(this, "download_channel")
                .setContentIntent(null)
                .setContentTitle("Paused")
                .setSmallIcon(R.drawable.ic_pause)
                .setStyle(NotificationCompat.BigTextStyle().bigText(video.assetId))
                .build()

            LuraOfflineEventType.FAILED -> notificationHelper.buildDownloadFailedNotification(
                this,
                com.akta.luraplayercontrols.R.drawable.error,
                null,
                video.assetId
            )

            else -> null
        }
        val id = video.assetId.hashCode()
        val notificationManager =
            this.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager?
        if (notification != null) {
            notificationManager?.notify(id, notification)
        } else {
            notificationManager?.cancel(id)
        }
    }

    override fun onDestroy() {
        offlineManager.removeListener(listener)
        super.onDestroy()
    }
}