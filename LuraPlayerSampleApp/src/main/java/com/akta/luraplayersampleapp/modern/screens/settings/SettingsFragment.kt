package com.akta.luraplayersampleapp.modern.screens.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.akta.luraplayer.api.enums.LuraDownloadRequirement
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.screens.settings.paralleldownloads.ParallelDownloadsSettingsFragment
import com.akta.luraplayersampleapp.modern.screens.settings.storage.StorageSettingsFragment
import com.akta.luraplayersampleapp.modern.screens.settings.videoquality.VideoQualitySettingsFragment
import com.akta.luraplayersampleapp.modern.utils.SharedPreferencesUtil
import com.akta.luraplayersampleapp.modern.custom.asHumanReadableBytes

class SettingsFragment : Fragment(R.layout.modern_fragment_settings) {

    companion object {
        const val TAG = "SettingsFragment"
    }

    private lateinit var wifiOnlyLayout: ConstraintLayout
    private lateinit var wifiOnlySwitch: SwitchCompat

    private lateinit var storageLayout: ConstraintLayout
    private lateinit var storageInfo: TextView

    private lateinit var parallelDownloadsLayout: ConstraintLayout
    private lateinit var parallelDownloadsInfo: TextView

    private lateinit var videoQualityLayout: ConstraintLayout
    private lateinit var videoQualityInfo: TextView

    private lateinit var luraOffline: LuraOfflineManager

    private lateinit var sharedPref: SharedPreferencesUtil

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        luraOffline = LuraOfflineManager(context = requireContext())

        wifiOnlyLayout = view.findViewById(R.id.wifi_only_layout)
        wifiOnlySwitch = view.findViewById(R.id.wifi_only_switch)

        storageLayout = view.findViewById(R.id.storage_layout)
        storageInfo = view.findViewById(R.id.storage_info)

        parallelDownloadsLayout = view.findViewById(R.id.parallel_downloads_layout)
        parallelDownloadsInfo = view.findViewById(R.id.parallel_downloads_info)

        videoQualityLayout = view.findViewById(R.id.video_quality_layout)
        videoQualityInfo = view.findViewById(R.id.video_quality_info)

        sharedPref = SharedPreferencesUtil.getInstance(requireContext())

        update()

        wifiOnlySwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.wifiOnly = isChecked

            val requirement =
                if (isChecked) LuraDownloadRequirement.WIFI else LuraDownloadRequirement.ANY
            luraOffline.setRequirements(requirement)
        }

        wifiOnlyLayout.setOnClickListener {
            wifiOnlySwitch.toggle()
        }

        storageLayout.setOnClickListener {
            parentFragmentManager.commit {
                add(
                    R.id.main_fragment,
                    StorageSettingsFragment(),
                    StorageSettingsFragment.TAG
                )
                addToBackStack(null)
            }
        }

        parallelDownloadsLayout.setOnClickListener {
            parentFragmentManager.commit {
                add(
                    R.id.main_fragment,
                    ParallelDownloadsSettingsFragment(),
                    ParallelDownloadsSettingsFragment.TAG
                )
                addToBackStack(null)
            }
        }

        videoQualityLayout.setOnClickListener {
            parentFragmentManager.commit {
                add(
                    R.id.main_fragment,
                    VideoQualitySettingsFragment(),
                    VideoQualitySettingsFragment.TAG
                )
                addToBackStack(null)
            }
        }
    }

    fun update() {
        wifiOnlySwitch.isChecked = sharedPref.wifiOnly
        storageInfo.text = sharedPref.maxStorage.asHumanReadableBytes()
        parallelDownloadsInfo.text = sharedPref.maxParallelDownloads.toString()
        videoQualityInfo.text = sharedPref.videoQuality
    }
}