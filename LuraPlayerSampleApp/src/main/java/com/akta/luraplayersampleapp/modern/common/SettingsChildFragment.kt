package com.akta.luraplayersampleapp.modern.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.screens.settings.SettingsFragment
import com.akta.luraplayersampleapp.modern.utils.SharedPreferencesUtil

open class SettingsChildFragment(layout: Int) : Fragment(layout) {

    lateinit var luraOffline: LuraOfflineManager

    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_from_end)
        exitTransition = inflater.inflateTransition(R.transition.slide_from_end)

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(true)

        sharedPreferencesUtil = SharedPreferencesUtil.getInstance(requireContext())
        luraOffline = LuraOfflineManager(context = requireContext())
    }

    override fun onDestroy() {
        parentFragmentManager.findFragmentByTag("SettingsFragment")?.let {
            (it as SettingsFragment).update()
        }

        super.onDestroy()

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowHomeEnabled(false)
    }
}