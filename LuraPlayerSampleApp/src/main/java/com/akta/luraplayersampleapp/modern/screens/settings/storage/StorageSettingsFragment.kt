package com.akta.luraplayersampleapp.modern.screens.settings.storage

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.common.SettingsChildFragment
import com.akta.luraplayersampleapp.modern.utils.SharedPreferencesUtil
import com.akta.luraplayersampleapp.modern.custom.asHumanReadableBytes
import com.akta.luraplayersampleapp.modern.custom.asHumanReadableBytesUnits
import com.akta.luraplayersampleapp.modern.custom.asHumanReadableBytesWithoutInfo
import com.akta.luraplayersampleapp.modern.custom.calculateTotalSize
import com.google.android.material.progressindicator.LinearProgressIndicator

// 500000000L = 500 MB
private const val SINGLE_STORAGE_STEP = 500000000L

class StorageSettingsFragment : SettingsChildFragment(R.layout.modern_fragment_storage_settings) {

    companion object {
        const val TAG = "StorageSettingsFragment"
    }

    private lateinit var decrementButton: CardView
    private lateinit var incrementButton: CardView
    private lateinit var storageIndicatorText: TextView
    private lateinit var storageSizeIndicatorText: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var usedText: TextView
    private lateinit var totalText: TextView

    private var occupiedStorage: Long = 0L

    private var maxAllowedStorage: Long = 0L
        set(value) {
            val withNumber = (value - occupiedStorage).asHumanReadableBytes()
            val withoutNumber = value.asHumanReadableBytesUnits()
            totalText.text = resources.getString(
                R.string.storage_remaining,
                withNumber
            )
            storageSizeIndicatorText.text = resources.getString(
                R.string.storage_info,
                withoutNumber
            )
            progressIndicator.progress = (100 * occupiedStorage / value).toInt()

            storageIndicatorText.text = value.asHumanReadableBytesWithoutInfo()
            sharedPreferencesUtil.maxStorage = value

            field = value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(requireContext())

        luraOffline = LuraOfflineManager(context = requireContext())

        decrementButton = view.findViewById(R.id.decrement_button)
        incrementButton = view.findViewById(R.id.increment_button)

        storageIndicatorText = view.findViewById(R.id.storage_text)
        storageSizeIndicatorText = view.findViewById(R.id.storage_size_indicator_text)

        progressIndicator = view.findViewById(R.id.progress_indicator)

        usedText = view.findViewById(R.id.used_text)
        totalText = view.findViewById(R.id.total_text)

        occupiedStorage = luraOffline.getVideos().calculateTotalSize()

        val maxStorage = sharedPreferencesUtil.maxStorage
        maxAllowedStorage = maxStorage

        usedText.text =
            resources.getString(R.string.storage_used, occupiedStorage.asHumanReadableBytes())

        decrementButton.setOnClickListener {
            if (maxAllowedStorage - SINGLE_STORAGE_STEP >= occupiedStorage
                && maxAllowedStorage - SINGLE_STORAGE_STEP > 0
            )
                maxAllowedStorage -= SINGLE_STORAGE_STEP
        }

        incrementButton.setOnClickListener {
            maxAllowedStorage += SINGLE_STORAGE_STEP
        }
    }
}