package com.akta.luraplayersampleapp.modern.screens.settings.videoquality

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.common.SettingsChildFragment

class VideoQualitySettingsFragment :
    SettingsChildFragment(R.layout.modern_fragment_video_quality_settings) {

    companion object {
        const val TAG = "VideoQualitySettingsFragment"
    }

    private lateinit var askEachTimeLayout: ConstraintLayout
    private lateinit var sdLayout: ConstraintLayout
    private lateinit var hdLayout: ConstraintLayout
    private lateinit var fullHDLayout: ConstraintLayout
    private lateinit var ultraHDLayout: ConstraintLayout

    private lateinit var askEachTimeSelectedIcon: ImageView
    private lateinit var sdSelectedIcon: ImageView
    private lateinit var hdSelectedIcon: ImageView
    private lateinit var fullHDSelectedIcon: ImageView
    private lateinit var ultraHDSelectedIcon: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        askEachTimeLayout = view.findViewById(R.id.ask_each_time_layout)
        sdLayout = view.findViewById(R.id.sd_layout)
        hdLayout = view.findViewById(R.id.hd_layout)
        fullHDLayout = view.findViewById(R.id.full_hd_layout)
        ultraHDLayout = view.findViewById(R.id.ultra_hd_layout)

        askEachTimeSelectedIcon = view.findViewById(R.id.ask_each_time_selected_icon)
        sdSelectedIcon = view.findViewById(R.id.sd_selected_icon)
        hdSelectedIcon = view.findViewById(R.id.hd_selected_icon)
        fullHDSelectedIcon = view.findViewById(R.id.full_hd_selected_icon)
        ultraHDSelectedIcon = view.findViewById(R.id.ultra_hd_selected_icon)

        when (sharedPreferencesUtil.videoQuality) {
            resources.getString(R.string.ask_each_time) -> {
                askEachTimeSelectedIcon.visibility = View.VISIBLE
            }

            resources.getString(R.string.sd) -> {
                sdSelectedIcon.visibility = View.VISIBLE
            }

            resources.getString(R.string.hd) -> {
                hdSelectedIcon.visibility = View.VISIBLE
            }

            resources.getString(R.string.full_hd) -> {
                fullHDSelectedIcon.visibility = View.VISIBLE
            }

            resources.getString(R.string.ultra_hd) -> {
                ultraHDSelectedIcon.visibility = View.VISIBLE
            }
        }

        askEachTimeLayout.setOnClickListener {
            resetSelectedLayout()
            sharedPreferencesUtil.videoQuality = resources.getString(R.string.ask_each_time)
            askEachTimeSelectedIcon.visibility = View.VISIBLE
        }

        sdLayout.setOnClickListener {
            resetSelectedLayout()
            sharedPreferencesUtil.videoQuality = resources.getString(R.string.sd)
            sdSelectedIcon.visibility = View.VISIBLE
        }

        hdLayout.setOnClickListener {
            resetSelectedLayout()
            sharedPreferencesUtil.videoQuality = resources.getString(R.string.hd)
            hdSelectedIcon.visibility = View.VISIBLE
        }

        fullHDLayout.setOnClickListener {
            resetSelectedLayout()
            sharedPreferencesUtil.videoQuality = resources.getString(R.string.full_hd)
            fullHDSelectedIcon.visibility = View.VISIBLE
        }

        ultraHDLayout.setOnClickListener {
            resetSelectedLayout()
            sharedPreferencesUtil.videoQuality = resources.getString(R.string.ultra_hd)
            ultraHDSelectedIcon.visibility = View.VISIBLE
        }
    }

    private fun resetSelectedLayout() {
        askEachTimeSelectedIcon.visibility = View.GONE
        sdSelectedIcon.visibility = View.GONE
        hdSelectedIcon.visibility = View.GONE
        fullHDSelectedIcon.visibility = View.GONE
        ultraHDSelectedIcon.visibility = View.GONE
    }
}