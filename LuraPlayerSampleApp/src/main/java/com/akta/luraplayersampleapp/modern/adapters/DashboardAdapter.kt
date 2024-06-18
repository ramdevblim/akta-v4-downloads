package com.akta.luraplayersampleapp.modern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.data.Video


class DashboardAdapter(
    private val videos: Map<String, List<Video>>
) : RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder>() {

    private val categories = videos.keys.toList()

    class DashboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView
        val contentPreviewRecyclerView: RecyclerView

        init {
            headerText = itemView.findViewById(R.id.header)
            contentPreviewRecyclerView = itemView.findViewById(R.id.asset_preview_recyclerview)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.modern_dashboard_item, parent, false)
        return DashboardViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: DashboardViewHolder,
        position: Int,
    ) {
        val category = categories[position]

        holder.headerText.text = category

        holder.contentPreviewRecyclerView.adapter =
            DashboardAssetPreviewAdapter(videos[category] ?: emptyList())
    }

    override fun getItemCount(): Int = videos.size
}