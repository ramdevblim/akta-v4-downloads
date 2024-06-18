package com.akta.luraplayersampleapp.modern.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.events.AssetSelectedEvent
import com.akta.luraplayersampleapp.modern.utils.Utils
import com.akta.luraplayersampleapp.modern.data.Video
import com.squareup.picasso.Picasso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.EventBus


class DashboardAssetPreviewAdapter(
    private val videos: List<Video>
) : RecyclerView.Adapter<DashboardAssetPreviewAdapter.DashboardAssetPreviewViewHolder>() {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    class DashboardAssetPreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assetPreviewImage: ImageView
        val assetTitleText: TextView
        val assetIDText: TextView

        init {
            assetPreviewImage = itemView.findViewById(R.id.asset_preview_image)
            assetTitleText = itemView.findViewById(R.id.asset_title)
            assetIDText = itemView.findViewById(R.id.asset_id)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DashboardAssetPreviewViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.modern_dashboard_asset_preview_item, parent, false)
        return DashboardAssetPreviewViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: DashboardAssetPreviewViewHolder,
        position: Int,
    ) {
        val video = videos[position]

        holder.assetTitleText.text = video.title
        holder.assetIDText.text = video.config.lura?.assetId

        val drawableID = Utils.getDrawableID(video.preview)

        Picasso.get()
            .load(drawableID)
            .placeholder(R.drawable.ic_placeholder).into(holder.assetPreviewImage)

        holder.itemView.setOnClickListener {
            EventBus.getDefault().post(
                AssetSelectedEvent(
                    json.encodeToString(videos),
                    position
                )
            )
        }
    }

    override fun getItemCount(): Int = videos.size
}