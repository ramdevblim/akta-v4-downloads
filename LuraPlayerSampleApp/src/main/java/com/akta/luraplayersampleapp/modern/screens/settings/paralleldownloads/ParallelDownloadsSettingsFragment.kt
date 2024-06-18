package com.akta.luraplayersampleapp.modern.screens.settings.paralleldownloads

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.common.SettingsChildFragment

class ParallelDownloadsSettingsFragment :
    SettingsChildFragment(R.layout.modern_fragment_parallel_downloads_settings) {

    companion object {
        const val TAG = "ParallelDownloadsSettingsFragment"
    }

    private lateinit var incrementButton: CardView
    private lateinit var decrementButton: CardView

    private lateinit var parallelDownloadsLimitText: TextView

    private var maxParallelDownload: Int = 0
        set(value) {
            parallelDownloadsLimitText.text = value.toString()
            sharedPreferencesUtil.maxParallelDownloads = value
            luraOffline.maxParallelDownloads = value
            field = value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        incrementButton = view.findViewById(R.id.increment_button)
        decrementButton = view.findViewById(R.id.decrement_button)

        parallelDownloadsLimitText = view.findViewById(R.id.parallel_downloads_text)

        maxParallelDownload = sharedPreferencesUtil.maxParallelDownloads
        parallelDownloadsLimitText.text = maxParallelDownload.toString()

        incrementButton.setOnClickListener {
            maxParallelDownload += 1
        }

        decrementButton.setOnClickListener {
            if (maxParallelDownload - 1 >= 1)
                maxParallelDownload -= 1
        }
    }
}