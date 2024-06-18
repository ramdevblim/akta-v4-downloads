package com.akta.luraplayercontrols.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.akta.luraplayer.api.models.LuraTrack
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.extensions.toBitrateName
import com.akta.luraplayercontrols.extensions.toLanguageName


internal class LuraTrackAdapter(
    internal var tracks: List<LuraTrack>,
    private val onItemClick: (LuraTrack) -> Unit,
) :
    RecyclerView.Adapter<LuraTrackAdapter.SubMenuViewHolder>() {
    internal var selectedIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubMenuViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.lura_sub_menu_item_view, parent, false)
        return SubMenuViewHolder(v)
    }

    override fun onBindViewHolder(holder: SubMenuViewHolder, position: Int) {
        val track = tracks[position]
        val (name, _) = when (track) {
            is LuraTrack.Video -> Pair(
                track.toBitrateName(context = holder.itemView.context),
                track.active
            )

            is LuraTrack.Audio -> Pair(track.toLanguageName(holder.itemView.context), track.active)
            is LuraTrack.Text -> Pair(
                track.toLanguageName(holder.itemView.context)
                    .ifEmpty { holder.itemView.context?.getString(R.string.luraOff) ?: "Off" },
                track.active
            )
        }
        if (position < tracks.size) {
            holder.textView.text = name
        }
        if (position == selectedIndex) {
            holder.itemView.isSelected = true
            holder.checkView.visibility = View.VISIBLE
        } else {
            holder.itemView.isSelected = false
            holder.checkView.visibility = View.INVISIBLE
        }
        holder.itemView.setOnClickListener {
            val absoluteAdapterPosition = holder.absoluteAdapterPosition
            selectedIndex = absoluteAdapterPosition
            onItemClick(tracks[absoluteAdapterPosition])
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return tracks.size
    }


    internal class SubMenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val textView: TextView
        internal val checkView: ImageView

        init {
            itemView.isFocusable = true
            textView = itemView.findViewById(R.id.luraSubMenuName)
            checkView = itemView.findViewById(R.id.luraSubMenuIcon)
        }
    }
}


