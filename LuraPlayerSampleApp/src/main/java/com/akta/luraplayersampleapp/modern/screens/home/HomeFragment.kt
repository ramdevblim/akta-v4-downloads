package com.akta.luraplayersampleapp.modern.screens.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.adapters.DashboardAdapter
import com.akta.luraplayersampleapp.modern.custom.getDashboardAssets

class HomeFragment : Fragment(R.layout.modern_fragment_home) {

    companion object {
        const val TAG = "HomeFragment"
    }

    private lateinit var recyclerView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val videos = requireContext().getDashboardAssets()

        recyclerView = view.findViewById(R.id.dashboard_recyclerview)
        val adapter = DashboardAdapter(videos)
        recyclerView.adapter = adapter
    }
}