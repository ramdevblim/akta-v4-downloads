package com.akta.luraplayersampleapp.modern.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayer.api.LuraOfflineEventListener
import com.akta.luraplayer.api.configs.offline.LuraOfflineConfiguration
import com.akta.luraplayer.api.enums.LuraDownloadingState
import com.akta.luraplayer.api.enums.LuraOfflineVideoResolution
import com.akta.luraplayer.api.logger.LuraLog
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayer.api.offline.LuraOfflineVideo
import com.akta.luraplayer.api.offline.event.LuraOfflineEventData
import com.akta.luraplayer.api.offline.event.LuraOfflineEventType
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.custom.ConfigurationSheet
import com.akta.luraplayersampleapp.modern.custom.ModernPopupMenuView
import com.akta.luraplayersampleapp.modern.custom.PieProgressBar
import com.akta.luraplayersampleapp.modern.custom.VideoDiffCallback
import com.akta.luraplayersampleapp.modern.dialogs.ConfirmationDialog
import com.akta.luraplayersampleapp.modern.dialogs.LuraAlertDialog
import com.akta.luraplayersampleapp.modern.events.AssetRefreshEvent
import com.akta.luraplayersampleapp.modern.utils.SharedPreferencesUtil
import com.akta.luraplayersampleapp.modern.utils.Utils
import com.akta.luraplayersampleapp.modern.data.Video
import com.akta.luraplayersampleapp.modern.custom.asHumanReadableBytes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class ModernVideoListAdapter(
    private var activity: Activity,
    private var luraOffline: LuraOfflineManager,
    private var videos: List<Video> = emptyList(),
    private val onStopClick: () -> Unit,
    private val onItemClick: (Video) -> Unit,
) : RecyclerView.Adapter<ModernVideoListAdapter.VideoListViewHolder>() {

    private var offlineWithDRMList: MutableMap<String, LuraOfflineVideo> = mutableMapOf()
    private var licenceCheckTimer: Timer? = null

    private var selectedPos: Int = RecyclerView.NO_POSITION

    private val context = activity.applicationContext

    private var shouldShowDialog = true

    private val popupWindow = PopupWindow(context).apply {
        isFocusable = true
    }

    private val downloadIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_download
    )
    private val moreIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_more
    )
    private val downloadDoneIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_download_done
    )
    private val errorIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_error
    )
    private val infoIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_info
    )
    private val renewIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_renew
    )
    private val trashIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_trash
    )
    private val queueIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_queue
    )
    private val warningIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_warning
    )
    private val pauseIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_action_pause
    )
    private val playIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_action_resume
    )
    private val stopIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_stop
    )

    private val naturalColor = ContextCompat.getColor(
        context,
        R.color.white
    )
    private val positiveColor = ContextCompat.getColor(
        context,
        R.color.green
    )
    private val warningColor = ContextCompat.getColor(
        context,
        R.color.warning
    )
    private val videoInformationColor = ContextCompat.getColor(
        context,
        R.color.video_information
    )
    private val errorColor = ContextCompat.getColor(
        context,
        R.color.red
    )

    private val popupMenuStats = context.resources.getString(R.string.popup_menu_item_stats)
    private val popupMenuDelete = context.resources.getString(R.string.popup_menu_item_delete)
    private val popupMenuDownload = context.resources.getString(R.string.popup_menu_item_download)
    private val popupMenuPause = context.resources.getString(R.string.popup_menu_item_pause)
    private val popupMenuRenew = context.resources.getString(R.string.popup_menu_item_renew)
    private val popupMenuResume = context.resources.getString(R.string.popup_menu_item_resume)
    private val popupMenuStop = context.resources.getString(R.string.stop)

    private val askEachTime = context.resources.getString(R.string.ask_each_time)
    private val sd = context.resources.getString(R.string.sd)
    private val hd = context.resources.getString(R.string.hd)
    private val fullHD = context.resources.getString(R.string.full_hd)
    private val ultraHD = context.resources.getString(R.string.ultra_hd)

    private val offlineFailed = context.resources.getString(R.string.offline_failed)
    private val warning = context.resources.getString(R.string.warning)
    private val confirmationTitleDelete =
        context.resources.getString(R.string.confirmation_title_delete)
    private val confirmationMessageDelete =
        context.resources.getString(R.string.confirmation_message_delete)

    @SuppressLint("NotifyDataSetChanged")
    private val listener: LuraOfflineEventListener = LuraOfflineEventListener { value ->
        delay(50L)

        when (val data = value.data) {
            is LuraOfflineEventData.Error -> {
                CoroutineScope(Dispatchers.Main).launch {
                    showDialog(message = data.error?.message)
                }
                updateIfNeeded(data.video, value.type)
            }

            is LuraOfflineEventData.Single -> {
                updateIfNeeded(data.video, value.type)
            }

            is LuraOfflineEventData.Multiple -> {
                data.videos.forEach { video -> updateIfNeeded(video, value.type) }
            }

            is LuraOfflineEventData.Warning -> {
                CoroutineScope(Dispatchers.Main).launch {
                    showDialog(title = warning, message = data.message)
                }
                if (data.video != null)
                    updateIfNeeded(data.video!!, value.type)
            }
        }
    }

    init {
        luraOffline.addListener(listener = listener)

        licenceCheckTimer = fixedRateTimer("update_drm_list", false, 30_000L, 30_000L) {
            updateOfflineWithDRMList()
        }
    }

    fun destroy() {
        licenceCheckTimer?.cancel()
        licenceCheckTimer = null

        removeListener()
    }

    fun setShowDialog(isEnabled: Boolean) {
        shouldShowDialog = isEnabled
    }

    fun updateVideos(newVideos: List<Video>, selectedPosition: Int) {
        val diffCallback = VideoDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videos = newVideos
        updateOfflineWithDRMList()

        selectedPos = selectedPosition

        diffResult.dispatchUpdatesTo(this)
    }

    private fun updateOfflineWithDRMList() {
        for (i in videos.indices) {
            val video = videos[i]

            luraOffline.getVideo(video.config)?.let { offlineVideo ->

                CoroutineScope(Dispatchers.IO).launch {

                    offlineVideo.withDRMLicense().let { offlineVideoWithDRM ->
                        offlineWithDRMList[offlineVideo.assetId] = offlineVideoWithDRM

                        CoroutineScope(Dispatchers.Main).launch {
                            notifyItemChanged(i)
                        }
                    }
                }
            }
        }
    }

    private fun updateIfNeeded(offline: LuraOfflineVideo, type: LuraOfflineEventType) {
        val indexToUpdate =
            videos.indexOfFirst { it.config.lura?.assetId == offline.assetId }
        if (indexToUpdate > -1L) {
            CoroutineScope(Dispatchers.Main).launch {
                notifyItemChanged(indexToUpdate, Pair(offline, type))
            }
        }
    }

    private fun showDialog(title: String? = offlineFailed, message: String?) {
        if (!shouldShowDialog) return

        LuraAlertDialog(activity, title ?: "NA", message ?: "NA").show()
    }

    private fun downloadVideo(video: Video, resolution: LuraOfflineVideoResolution) {
        if (video.config.lura == null) return

        CoroutineScope(Dispatchers.IO).launch {
            luraOffline.download(
                video.config.copy(offlinePlayback = LuraOfflineConfiguration(resolution = resolution))
            )
            EventBus.getDefault().post(AssetRefreshEvent())
        }
    }

    private fun resumeVideoDownload(video: Video) {
        if (video.config.lura == null) return
        CoroutineScope(Dispatchers.IO).launch {
            luraOffline.resume(video.config.copy(offlinePlayback = LuraOfflineConfiguration()))
        }
    }

    private fun pauseVideoDownload(video: Video) {
        if (video.config.lura == null) return
        CoroutineScope(Dispatchers.IO).launch {
            luraOffline.pause(video.config.copy())
        }
    }

    private fun removeVideo(video: Video) {
        if (video.config.lura == null) return

        CoroutineScope(Dispatchers.Main).launch {
            ConfirmationDialog(
                activity,
                confirmationTitleDelete,
                confirmationMessageDelete,
                {
                    offlineWithDRMList.remove(video.config.lura?.assetId)

                    CoroutineScope(Dispatchers.IO).launch {
                        luraOffline.remove(video.config.copy(offlinePlayback = LuraOfflineConfiguration()))
                    }
                }).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showActionSheet(video: Video) {
        try {
            val quality = SharedPreferencesUtil.getInstance(context).videoQuality

            if (quality != askEachTime) {
                val resolution = when (quality) {
                    sd -> {
                        LuraOfflineVideoResolution.SD
                    }

                    hd -> {
                        LuraOfflineVideoResolution.HD
                    }

                    fullHD -> {
                        LuraOfflineVideoResolution.FullHD
                    }

                    ultraHD -> {
                        LuraOfflineVideoResolution.UltraHD
                    }

                    else -> {
                        LuraOfflineVideoResolution.SD
                    }
                }

                downloadVideo(video, resolution)
                return
            }

            val sheetView: View =
                activity.layoutInflater.inflate(R.layout.modern_action_sheet, null)
            val actionSheet = BottomSheetDialog(activity)
            actionSheet.setContentView(sheetView)

            val view =
                actionSheet.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            view?.background = null

            sheetView.findViewById<FrameLayout>(R.id.option_sd).setOnClickListener {
                downloadVideo(video, LuraOfflineVideoResolution.SD)
                actionSheet.dismiss()
            }

            sheetView.findViewById<FrameLayout>(R.id.option_hd).setOnClickListener {
                downloadVideo(video, LuraOfflineVideoResolution.HD)
                actionSheet.dismiss()
            }

            sheetView.findViewById<FrameLayout>(R.id.option_full_hd).setOnClickListener {
                downloadVideo(video, LuraOfflineVideoResolution.FullHD)
                actionSheet.dismiss()
            }

            sheetView.findViewById<FrameLayout>(R.id.option_ultra_hd).setOnClickListener {
                downloadVideo(video, LuraOfflineVideoResolution.UltraHD)
                actionSheet.dismiss()
            }

            sheetView.findViewById<FrameLayout>(R.id.option_close).setOnClickListener {
                actionSheet.dismiss()
            }

            actionSheet.show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun showAnchorDialog(
        anchorView: View,
        menuItems: List<Pair<String, Drawable>>,
        position: Int
    ) {
        if (popupWindow.isShowing) popupWindow.dismiss()

        val video = videos[position]

        val adapter = ModernPopupMenuAdapter(context, menuItems) { item ->
            when (item) {
                popupMenuDownload -> {
                    showActionSheet(video)
                }

                popupMenuStats -> {
                    luraOffline.getVideo(video.config)?.withDRMLicense()?.let {
                        ConfigurationSheet(activity, it).show()
                    }
                }

                popupMenuRenew -> {
                    luraOffline.updateLicense(video.config)
                }

                popupMenuDelete -> {
                    removeVideo(video)
                }

                popupMenuPause -> {
                    pauseVideoDownload(video)
                }

                popupMenuResume -> {
                    resumeVideoDownload(video)
                }

                popupMenuStop -> {
                    onStopClick.invoke()
                }
            }

            popupWindow.dismiss()
        }

        val menuView: ModernPopupMenuView = ModernPopupMenuView(context).apply {
            updateAdapter(adapter)
        }

        popupWindow.contentView = menuView
        popupWindow.showAsDropDown(anchorView)
    }

    class VideoListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView
        val assetID: TextView
        val infoSeparator: TextView
        val stateText: TextView
        val progressText: TextView
        val moreButton: CardView
        val moreButtonIcon: ImageView
        val sizeIndicator: TextView
        val progressContainer: FrameLayout
        val progressBar: PieProgressBar
        val stateIndicator: ImageView
        val thumbnailImage: ImageView

        init {
            title = itemView.findViewById(R.id.title)
            assetID = itemView.findViewById(R.id.asset_id)
            infoSeparator = itemView.findViewById(R.id.information_separator)
            stateText = itemView.findViewById(R.id.state_text)
            progressText = itemView.findViewById(R.id.progress_text)
            moreButton = itemView.findViewById(R.id.more_button)
            moreButtonIcon = itemView.findViewById(R.id.more_button_icon)
            sizeIndicator = itemView.findViewById(R.id.size_indicator)
            progressContainer = itemView.findViewById(R.id.progress_layout)
            progressBar = itemView.findViewById(R.id.progress_bar)
            stateIndicator = itemView.findViewById(R.id.state_indicator)
            thumbnailImage = itemView.findViewById(R.id.thumbnail)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.modern_video_list_item, parent, false)
        return VideoListViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: VideoListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            this.onBindViewHolder(holder, position)
        } else {
            if (payloads[0] is Boolean) {
                holder.itemView.isSelected = payloads[0] as Boolean
                return
            }
            val (offline, type) = payloads[0] as Pair<*, *>
            if (offline != null && offline is LuraOfflineVideo && type is LuraOfflineEventType) {
                var state = when (type) {
                    LuraOfflineEventType.QUEUED -> ItemState.QUEUED
                    LuraOfflineEventType.STOPPED -> ItemState.STOPPED
                    LuraOfflineEventType.PAUSED -> ItemState.PAUSED
                    LuraOfflineEventType.PAUSED_BY_REQUIREMENTS -> ItemState.PAUSED_BY_REQUIREMENTS
                    LuraOfflineEventType.STARTING -> ItemState.STARTING
                    LuraOfflineEventType.DOWNLOADING -> ItemState.DOWNLOADING
                    LuraOfflineEventType.PROGRESS -> ItemState.PROGRESS
                    LuraOfflineEventType.COMPLETED -> ItemState.COMPLETED
                    LuraOfflineEventType.FAILED -> ItemState.FAILED
                    LuraOfflineEventType.WARNING -> ItemState.WARNING
                    LuraOfflineEventType.REMOVING -> ItemState.REMOVING
                    LuraOfflineEventType.REMOVING_ALL -> ItemState.REMOVING_ALL
                    LuraOfflineEventType.REMOVED -> ItemState.REMOVED
                    LuraOfflineEventType.RESUMED_ALL -> ItemState.RESUMED_ALL
                    LuraOfflineEventType.PAUSED_ALL -> ItemState.PAUSED_ALL
                    LuraOfflineEventType.RESTARTING -> ItemState.RESTARTING
                    LuraOfflineEventType.LICENSE_UPDATING -> ItemState.LICENSE_UPDATING
                    LuraOfflineEventType.LICENSE_UPDATED -> ItemState.LICENSE_UPDATED
                    LuraOfflineEventType.LICENSE_UPDATING_FAILED -> ItemState.LICENSE_UPDATING_FAILED
                }
                if (state == ItemState.COMPLETED) {
                    offlineWithDRMList[offline.assetId]?.let {
                        if (it.license != null &&
                            (it.license!!.licenseExpirationDate == 0L ||
                                    it.license!!.totalPlaybackDuration == 0L)
                        ) {
                            state = ItemState.LICENSE_EXPIRED
                        }
                    }
                }
                setUIState(holder, state, position, offline)
            }
        }
    }

    override fun onBindViewHolder(
        holder: VideoListViewHolder,
        @SuppressLint("RecyclerView") position: Int,
    ) {
        val video = videos[position]

        holder.title.text = video.title
        holder.assetID.text = video.config.lura?.assetId ?: "NA"

        holder.itemView.isSelected = selectedPos == position
        holder.itemView.setOnClickListener {
            if (selectedPos != position) {
                notifyItemChanged(selectedPos, false)
                selectedPos = position
                notifyItemChanged(selectedPos, true)
            }
            onItemClick(video)
        }

        holder.itemView.setOnLongClickListener {
            if (selectedPos == position) {
                showAnchorDialog(
                    holder.itemView,
                    listOf(Pair(popupMenuStop, stopIcon!!)),
                    position
                )
            }
            return@setOnLongClickListener true
        }

        val drawableID = Utils.getDrawableID(video.preview)

        Picasso.get()
            .load(drawableID)
            .placeholder(R.drawable.ic_placeholder).into(holder.thumbnailImage)

        setUI(holder, video, position)
    }

    override fun getItemCount(): Int = videos.size

    private fun removeListener() {
        luraOffline.removeListener(listener)
    }

    private fun setUI(holder: VideoListViewHolder, video: Video? = null, position: Int) {
        val offline: LuraOfflineVideo?
        var isDownloadable = false

        if (video == null) {
            offline = null
        } else {
            offline = luraOffline.getVideo(video.config)
            isDownloadable = true// video.title?.contains("(DW)") ?: false
        }

        var state = if (isDownloadable) ItemState.DOWNLOADABLE else ItemState.NOT_DOWNLOADABLE

        if (state == ItemState.DOWNLOADABLE) {
            state = when (offline?.state) {
                LuraDownloadingState.COMPLETED -> ItemState.COMPLETED
                LuraDownloadingState.DOWNLOADING -> ItemState.DOWNLOADING
                LuraDownloadingState.FAILED -> ItemState.FAILED
                LuraDownloadingState.QUEUED -> ItemState.QUEUED
                LuraDownloadingState.REMOVING -> ItemState.REMOVING
                LuraDownloadingState.RESTARTING -> ItemState.RESTARTING
                LuraDownloadingState.STOPPED -> ItemState.STOPPED
                LuraDownloadingState.PAUSED -> ItemState.PAUSED
                LuraDownloadingState.PAUSED_BY_REQUIREMENTS -> ItemState.PAUSED_BY_REQUIREMENTS
                else -> ItemState.DOWNLOADABLE
            }
        }

        if (state == ItemState.COMPLETED) {
            offlineWithDRMList[offline?.assetId]?.let {
                if (it.license != null &&
                    (it.license!!.licenseExpirationDate == 0L ||
                            it.license!!.totalPlaybackDuration == 0L)
                ) {
                    state = ItemState.LICENSE_EXPIRED
                }
            }
        }

        setUIState(holder, state, position, offline)
    }

    private fun setUIState(
        holder: VideoListViewHolder,
        state: ItemState,
        position: Int,
        offline: LuraOfflineVideo? = null
    ) {
        var stateIndicatorVisibility: Int = View.GONE
        var stateIndicatorDrawable: Drawable = downloadIcon!!
        var stateIndicatorColor: Int = naturalColor
        var stateTextVisibility: Int = View.GONE
        var stateText = ""
        var stateTextColor: Int = naturalColor
        var progressTextVisibility: Int = View.GONE
        var progressText = ""

        var progressContainerVisibility: Int = View.GONE
        var progressIndicatorValue = 0

        var moreButtonVisibility: Int = View.GONE
        var moreButtonDrawable: Drawable = moreIcon!!
        var sizeIndicatorVisibility: Int = View.GONE
        var sizeIndicatorText = ""
        var infoSeparatorVisibility = View.GONE

        var action: (() -> Unit)? = null
        LuraLog.d("LuraAdapter", "$state")
        when (state) {
            ItemState.NOT_DOWNLOADABLE -> {

            }

            ItemState.REMOVED,
            ItemState.DOWNLOADABLE -> {
                moreButtonVisibility = View.VISIBLE
                moreButtonDrawable = downloadIcon

                action = {
                    showActionSheet(videos[position])
                }
            }

            ItemState.STARTING -> {
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.starting)
                progressTextVisibility = View.VISIBLE
                progressText = context.getString(R.string.offline_progress, 0)
                infoSeparatorVisibility = View.VISIBLE

                progressContainerVisibility = View.VISIBLE
                progressIndicatorValue = 0

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuPause, pauseIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.PAUSED -> {
                val progress = offline?.progress?.roundToInt() ?: -1

                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = pauseIcon!!
                stateIndicatorColor = videoInformationColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.paused)
                stateTextColor = videoInformationColor
                progressTextVisibility = View.VISIBLE
                progressText = context.getString(R.string.offline_progress, progress)
                infoSeparatorVisibility = View.VISIBLE

                progressContainerVisibility = View.VISIBLE
                progressIndicatorValue = progress

                moreButtonVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuResume, playIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.PAUSED_BY_REQUIREMENTS -> {
                val progress = offline?.progress?.roundToInt() ?: -1

                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = warningIcon!!
                stateIndicatorColor = warningColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.network_requirement_not_met)
                stateTextColor = warningColor
                progressTextVisibility = View.VISIBLE
                progressText = context.getString(R.string.offline_progress, progress)
                infoSeparatorVisibility = View.VISIBLE

                progressContainerVisibility = View.VISIBLE
                progressIndicatorValue = progress

                moreButtonVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuResume, playIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.STOPPED,
            ItemState.PAUSED_ALL,
            ItemState.QUEUED -> {
                val progress = offline?.progress?.roundToInt() ?: -1

                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = queueIcon!!
                stateIndicatorColor = videoInformationColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.queued)
                stateTextColor = videoInformationColor
                progressTextVisibility = View.VISIBLE
                progressText = context.getString(R.string.offline_progress, progress)
                infoSeparatorVisibility = View.VISIBLE

                progressContainerVisibility = View.VISIBLE
                progressIndicatorValue = progress

                moreButtonVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuPause, pauseIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.RESUMED_ALL,
            ItemState.DOWNLOADING,
            ItemState.PROGRESS -> {
                val progress = offline?.progress?.roundToInt() ?: -1

                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = downloadIcon
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.downloading)
                progressTextVisibility = View.VISIBLE
                progressText = context.getString(R.string.offline_progress, progress)
                progressContainerVisibility = View.VISIBLE
                progressIndicatorValue = progress
                infoSeparatorVisibility = View.VISIBLE

                moreButtonVisibility = View.VISIBLE
                infoSeparatorVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuPause, pauseIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.LICENSE_EXPIRED -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = warningIcon!!
                stateIndicatorColor = warningColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.licence_expired)
                stateTextColor = warningColor
                infoSeparatorVisibility = View.VISIBLE

                moreButtonVisibility = View.VISIBLE
                infoSeparatorVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuRenew, renewIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.LICENSE_UPDATED,
            ItemState.COMPLETED -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = downloadDoneIcon!!
                stateIndicatorColor = positiveColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.downloaded)
                stateTextColor = positiveColor
                infoSeparatorVisibility = View.VISIBLE

                moreButtonVisibility = View.VISIBLE
                infoSeparatorVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.FAILED -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = errorIcon!!
                stateIndicatorColor = errorColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.error)
                stateTextColor = errorColor

                moreButtonVisibility = View.VISIBLE

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuDownload, downloadIcon)
                        ),
                        position
                    )
                }
            }

            ItemState.REMOVING_ALL,
            ItemState.REMOVING -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = trashIcon!!
                stateIndicatorColor = videoInformationColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.removing)
                stateTextColor = videoInformationColor
            }

            ItemState.LICENSE_UPDATING -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = renewIcon!!
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.licence_updating)
                infoSeparatorVisibility = View.VISIBLE

                moreButtonVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"
            }

            ItemState.LICENSE_UPDATING_FAILED -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = warningIcon!!
                stateIndicatorColor = warningColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.licence_update_failed)
                stateTextColor = warningColor
                infoSeparatorVisibility = View.VISIBLE

                moreButtonVisibility = View.VISIBLE
                infoSeparatorVisibility = View.VISIBLE
                sizeIndicatorVisibility = View.VISIBLE
                sizeIndicatorText = offline?.bytes?.asHumanReadableBytes() ?: "NA"

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuStats, infoIcon!!),
                            Pair(popupMenuRenew, renewIcon!!),
                            Pair(popupMenuDelete, trashIcon!!),
                        ),
                        position
                    )
                }
            }

            ItemState.RESTARTING -> {

            }

            ItemState.NONE -> {

            }

            ItemState.WARNING -> {
                stateIndicatorVisibility = View.VISIBLE
                stateIndicatorDrawable = warningIcon!!
                stateIndicatorColor = warningColor
                stateTextVisibility = View.VISIBLE
                stateText = context.getString(R.string.warning)
                stateTextColor = warningColor

                moreButtonVisibility = View.VISIBLE

                action = {
                    showAnchorDialog(
                        holder.moreButton,
                        listOf(
                            Pair(popupMenuDownload, downloadIcon)
                        ),
                        position
                    )
                }
            }
        }

        holder.apply {
            stateIndicator.visibility = stateIndicatorVisibility
            stateIndicator.setImageDrawable(stateIndicatorDrawable)
            stateIndicator.setColorFilter(stateIndicatorColor)
            this.stateText.visibility = stateTextVisibility
            this.stateText.text = stateText
            this.stateText.setTextColor(stateTextColor)
            this.progressText.visibility = progressTextVisibility
            this.progressText.text = progressText
            infoSeparator.visibility = infoSeparatorVisibility

            progressContainer.visibility = progressContainerVisibility
            progressBar.setProgress(progressIndicatorValue)

            sizeIndicator.visibility = sizeIndicatorVisibility
            sizeIndicator.text = sizeIndicatorText

            moreButton.visibility = moreButtonVisibility
            this.moreButtonIcon.setImageDrawable(moreButtonDrawable)
            moreButton.setOnClickListener {
                action?.invoke()
            }
        }
    }

    private enum class ItemState {
        NOT_DOWNLOADABLE,
        DOWNLOADABLE,
        NONE,
        QUEUED,
        STOPPED,
        PAUSED,
        PAUSED_BY_REQUIREMENTS,
        STARTING,
        DOWNLOADING,
        PROGRESS,
        COMPLETED,
        FAILED,
        WARNING,
        REMOVING,
        REMOVING_ALL,
        REMOVED,
        RESUMED_ALL,
        PAUSED_ALL,
        RESTARTING,
        LICENSE_EXPIRED,
        LICENSE_UPDATING,
        LICENSE_UPDATED,
        LICENSE_UPDATING_FAILED,
    }

}
