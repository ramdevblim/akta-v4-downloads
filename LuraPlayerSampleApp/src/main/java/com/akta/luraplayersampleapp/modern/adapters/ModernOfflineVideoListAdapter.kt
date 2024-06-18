package com.akta.luraplayersampleapp.modern.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayer.api.LuraOfflineEventListener
import com.akta.luraplayer.api.configs.offline.LuraOfflineConfiguration
import com.akta.luraplayer.api.enums.LuraDownloadingState
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
import com.akta.luraplayersampleapp.modern.events.AssetSelectedEvent
import com.akta.luraplayersampleapp.modern.utils.Utils
import com.akta.luraplayersampleapp.modern.data.Video
import com.akta.luraplayersampleapp.modern.custom.asHumanReadableBytes
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.EventBus
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt

class ModernOfflineVideoListAdapter(
    private var activity: Activity,
    private var luraOffline: LuraOfflineManager,
    private var videos: List<Video> = emptyList()
) : RecyclerView.Adapter<ModernOfflineVideoListAdapter.VideoListViewHolder>() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val context = activity.applicationContext

    private var offlineWithDRMList: MutableMap<String, LuraOfflineVideo> = mutableMapOf()

    private var actionMode: ActionMode? = null
    private var isSelectionMode = false
    private val selectedVideos: MutableSet<Video> = mutableSetOf()

    private var shouldShowDialog = true

    private var licenceCheckTimer: Timer? = null

    private val popupWindow = PopupWindow(context).apply {
        isFocusable = true
    }

    private val selectedIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_selected
    )
    private val downloadIcon = ContextCompat.getDrawable(
        context,
        R.drawable.ic_download
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
    private val actionModeColor = ContextCompat.getColor(
        context,
        R.color.action_mode_background
    )
    private val blackColor = ContextCompat.getColor(
        context,
        R.color.black
    )

    private val popupMenuStats = context.resources.getString(R.string.popup_menu_item_stats)
    private val popupMenuDelete = context.resources.getString(R.string.popup_menu_item_delete)
    private val popupMenuDownload = context.resources.getString(R.string.popup_menu_item_download)
    private val popupMenuPause = context.resources.getString(R.string.popup_menu_item_pause)
    private val popupMenuRenew = context.resources.getString(R.string.popup_menu_item_renew)
    private val popupMenuResume = context.resources.getString(R.string.popup_menu_item_resume)

    private val offlineFailed = context.resources.getString(R.string.offline_failed)
    private val confirmationTitleDelete =
        context.resources.getString(R.string.confirmation_title_delete)
    private val confirmationMessageDelete =
        context.resources.getString(R.string.confirmation_message_delete)
    private val confirmationTitleDeleteMultiple =
        context.resources.getString(R.string.confirmation_title_delete_multiple)
    private val confirmationMessageDeleteMultiple =
        context.resources.getString(R.string.confirmation_message_delete_multiple)

    @SuppressLint("NotifyDataSetChanged")
    private val listener: LuraOfflineEventListener = LuraOfflineEventListener { value ->
        delay(50L)

        when (val data = value.data) {
            is LuraOfflineEventData.Error -> {
                CoroutineScope(Dispatchers.Main).launch {
                    showDialog(data.error?.message)
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
                    showDialog(data.message)
                }
                if (data.video != null)
                    updateIfNeeded(data.video!!, value.type)
            }
        }
    }

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        @SuppressLint("NotifyDataSetChanged")
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val inflater = mode?.menuInflater
            inflater?.inflate(R.menu.menu_offline_actions, menu)

            for (i in 0 until menu!!.size()) {
                val item = menu.getItem(i)

                if (item.itemId == R.id.menu_item_select_all) {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
                } else {
                    item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    item.isVisible = selectedVideos.size > 0
                }
            }

            activity.window.statusBarColor = actionModeColor

            isSelectionMode = true

            actionMode = mode

            notifyDataSetChanged()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.menu_item_delete -> {
                    ConfirmationDialog(
                        activity,
                        confirmationTitleDeleteMultiple,
                        confirmationMessageDeleteMultiple,
                        {
                            selectedVideos.forEach {
                                removeVideo(it, true)
                            }
                            mode?.finish()
                        }).show()
                }

                R.id.menu_item_pause -> {
                    selectedVideos.forEach {
                        pauseVideoDownload(it)
                    }
                    mode?.finish()
                }

                R.id.menu_item_resume -> {
                    selectedVideos.forEach {
                        resumeVideoDownload(it)
                    }
                    mode?.finish()
                }

                R.id.menu_item_select_all -> {
                    selectedVideos.clear()
                    selectedVideos.addAll(videos)

                    if (selectedVideos.size > 0)
                        actionMode?.menu?.forEach { it.isVisible = true }

                    notifyDataSetChanged()
                }
            }

            return true
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            activity.window.statusBarColor = blackColor

            actionMode = null

            isSelectionMode = false

            selectedVideos.clear()
            notifyDataSetChanged()
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

        actionMode?.finish()

        removeListener()
    }

    fun setShowDialog(isEnabled: Boolean) {
        shouldShowDialog = isEnabled
    }

    fun updateVideos(newVideos: List<Video>) {
        val diffCallback = VideoDiffCallback(videos, newVideos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        videos = newVideos
        updateOfflineWithDRMList()

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

    private fun showDialog(msg: String?) {
        if (!shouldShowDialog) return

        LuraAlertDialog(activity, offlineFailed, msg ?: "").show()
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

    private fun removeVideo(video: Video, overrideConfirmation: Boolean = false) {
        if (video.config.lura == null) return

        fun remove() {
            offlineWithDRMList.remove(video.config.lura?.assetId)

            CoroutineScope(Dispatchers.IO).launch {
                luraOffline.remove(video.config.copy(offlinePlayback = LuraOfflineConfiguration()))
            }
        }

        if (overrideConfirmation) {
            remove()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            ConfirmationDialog(
                activity,
                confirmationTitleDelete,
                confirmationMessageDelete,
                {
                    remove()
                }).show()
        }
    }

    fun startActionMode(video: Video? = null) {
        if (videos.isEmpty()) return

        if (video != null)
            toggleSelection(video)

        activity.startActionMode(actionModeCallback)
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
                popupMenuStats -> {
                    luraOffline.getVideo(videos[position].config)?.withDRMLicense()?.let {
                        ConfigurationSheet(activity, it).show()
                    }
                }

                popupMenuRenew -> {
                    luraOffline.updateLicense(videos[position].config)
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
            }

            popupWindow.dismiss()
        }

        val menuView: ModernPopupMenuView = ModernPopupMenuView(context).apply {
            updateAdapter(adapter)
        }

        popupWindow.contentView = menuView
        popupWindow.showAsDropDown(anchorView)
    }

    private fun toggleSelection(video: Video) {
        if (selectedVideos.contains(video))
            selectedVideos.remove(video)
        else
            selectedVideos.add(video)

        val index = videos.indexOf(video)
        notifyItemChanged(index)

        if (selectedVideos.size == 0)
            actionMode?.finish()
        else
            actionMode?.menu?.forEach { it.isVisible = true }
    }

    class VideoListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView
        val assetID: TextView
        val infoSeparator: TextView
        val stateText: TextView
        val progressText: TextView
        val moreButton: CardView
        val sizeIndicator: TextView
        val progressContainer: FrameLayout
        val progressBar: PieProgressBar
        val stateIndicator: ImageView
        val thumbnailImage: ImageView
        val selectedImage: ImageView

        init {
            title = itemView.findViewById(R.id.title)
            assetID = itemView.findViewById(R.id.asset_id)
            infoSeparator = itemView.findViewById(R.id.information_separator)
            stateText = itemView.findViewById(R.id.state_text)
            progressText = itemView.findViewById(R.id.progress_text)
            moreButton = itemView.findViewById(R.id.more_button)
            sizeIndicator = itemView.findViewById(R.id.size_indicator)
            progressContainer = itemView.findViewById(R.id.progress_layout)
            progressBar = itemView.findViewById(R.id.progress_bar)
            stateIndicator = itemView.findViewById(R.id.state_indicator)
            thumbnailImage = itemView.findViewById(R.id.thumbnail)
            selectedImage = itemView.findViewById(R.id.checkmark)
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
                if (state == ItemState.REMOVED) {
                    EventBus.getDefault().post(
                        AssetRefreshEvent(position)
                    )
                    selectedVideos.remove(videos[position])
                } else {
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
    }

    override fun onBindViewHolder(
        holder: VideoListViewHolder,
        @SuppressLint("RecyclerView") position: Int,
    ) {
        val video = videos[position]

        holder.title.text = video.title
        holder.assetID.text = video.config.lura?.assetId ?: "NA"

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(video)
            } else {
                EventBus.getDefault().post(
                    AssetSelectedEvent(
                        json.encodeToString(listOf(video)),
                        0
                    )
                )
            }
        }

        holder.itemView.setOnLongClickListener {
            if (isSelectionMode) return@setOnLongClickListener true

            startActionMode(video)

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
        val offline: LuraOfflineVideo? = video?.let {
            luraOffline.getVideo(it.config)
        }

        var state = when (offline?.state) {
            LuraDownloadingState.COMPLETED -> ItemState.COMPLETED
            LuraDownloadingState.DOWNLOADING -> ItemState.DOWNLOADING
            LuraDownloadingState.FAILED -> ItemState.FAILED
            LuraDownloadingState.QUEUED -> ItemState.QUEUED
            LuraDownloadingState.REMOVING -> ItemState.REMOVING
            LuraDownloadingState.RESTARTING -> ItemState.RESTARTING
            LuraDownloadingState.STOPPED -> ItemState.STOPPED
            LuraDownloadingState.PAUSED -> ItemState.PAUSED
            LuraDownloadingState.PAUSED_BY_REQUIREMENTS -> ItemState.PAUSED_BY_REQUIREMENTS
            else -> ItemState.NONE
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
        var sizeIndicatorVisibility: Int = View.GONE
        var sizeIndicatorText = ""
        var infoSeparatorVisibility = View.GONE

        var action: (() -> Unit)? = null

        when (state) {
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

            else -> {}
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

            if (isSelectionMode) {
                selectedImage.visibility = View.VISIBLE

                if (selectedVideos.contains(videos[position]))
                    selectedImage.setImageDrawable(selectedIcon!!)
                else
                    selectedImage.setImageDrawable(null)
            } else {
                selectedImage.visibility = View.GONE
                selectedImage.setImageDrawable(null)
            }

            moreButton.visibility = moreButtonVisibility
            moreButton.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(videos[position])
                } else {
                    action?.invoke()
                }
            }
        }
    }

    private enum class ItemState {
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
