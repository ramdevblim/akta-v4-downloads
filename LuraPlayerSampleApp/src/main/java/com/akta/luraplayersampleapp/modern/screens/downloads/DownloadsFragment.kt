package com.akta.luraplayersampleapp.modern.screens.downloads

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.adapters.ModernOfflineVideoListAdapter
import com.akta.luraplayersampleapp.modern.custom.getDashboardAssets
import com.akta.luraplayersampleapp.modern.events.AssetRefreshEvent
import com.akta.luraplayersampleapp.modern.events.EditButtonPressedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class DownloadsFragment : Fragment(R.layout.modern_fragment_downloads) {

    companion object {
        const val TAG = "DownloadsFragment"
    }

    private lateinit var videoListRecyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout

    private lateinit var luraOffline: LuraOfflineManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        luraOffline = LuraOfflineManager(context = requireContext())

        emptyView = view.findViewById(R.id.empty_view)
        videoListRecyclerView = view.findViewById(R.id.video_list_recyclerview)

        videoListRecyclerView.setHasFixedSize(true)
        videoListRecyclerView.adapter = ModernOfflineVideoListAdapter(
            activity = requireActivity(),
            luraOffline = luraOffline
        )
    }

    private fun fetchVideos() {
        val offlineVideoIDs = luraOffline.getVideos().map { it.assetId }.toSet()

        val videos = requireContext().getDashboardAssets().values.flatten().filter {
            it.config.lura?.assetId in offlineVideoIDs
        }.filter { it.title?.contains("(DW)") == true }

        if (videos.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            videoListRecyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            videoListRecyclerView.visibility = View.VISIBLE

            videoListRecyclerView.post {
                (videoListRecyclerView.adapter as ModernOfflineVideoListAdapter).updateVideos(videos)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAssetRefreshEvent(event: AssetRefreshEvent) {
        fetchVideos()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEditButtonPressedEvent(event: EditButtonPressedEvent) {
        (videoListRecyclerView.adapter as ModernOfflineVideoListAdapter).startActionMode()
    }

    override fun onResume() {
        super.onResume()

        fetchVideos()

        (videoListRecyclerView.adapter as ModernOfflineVideoListAdapter).setShowDialog(true)
    }

    override fun onPause() {
        super.onPause()

        (videoListRecyclerView.adapter as ModernOfflineVideoListAdapter).setShowDialog(false)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        (videoListRecyclerView.adapter as ModernOfflineVideoListAdapter).destroy()
        super.onDestroy()
    }

}