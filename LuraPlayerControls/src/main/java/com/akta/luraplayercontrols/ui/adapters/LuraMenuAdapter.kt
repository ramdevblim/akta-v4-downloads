package com.akta.luraplayercontrols.ui.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayercontrols.R

import com.akta.luraplayercontrols.ui.LuraPopupType

internal class LuraMenuAdapter(
    private val mainTexts: Array<String>,
    private val iconIds: Array<Drawable?>,
    internal val type: LuraPopupType,
    private val onItemClick: (Int) -> Unit,
) :
    RecyclerView.Adapter<LuraMenuAdapter.SettingViewHolder>() {
    private val subTexts: Array<String?> = arrayOfNulls(mainTexts.size)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.lura_menu_item_view, parent, false)
        return SettingViewHolder(v)
    }

    override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
        holder.itemView.layoutParams =
            if (subTexts[position]?.isEmpty() == true)
                RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0
                )
            else
                RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
        holder.itemView.isEnabled = subTexts[position]?.isEmpty() != true

        holder.mainTextView.text = mainTexts[position]
        if (subTexts[position] == null) {
            holder.subTextView.visibility = View.INVISIBLE
        } else {
            holder.subTextView.text = subTexts[position]
        }
        if (iconIds.isEmpty() || iconIds[position] == null) {
            if (type == LuraPopupType.CaptionsAndAudios || type == LuraPopupType.CaptionSettings) {
                holder.iconView.visibility = View.GONE
            } else {
                holder.iconView.visibility = View.INVISIBLE
            }
        } else {
            holder.iconView.setImageDrawable(iconIds[position])
        }
        holder.itemView.setOnClickListener {
            onItemClick(holder.absoluteAdapterPosition)
        }

    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return mainTexts.size
    }

    fun setSubTextAtPosition(position: Int, subText: String?) {
        subTexts[position] = subText
    }

    internal class SettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val mainTextView: TextView
        internal val subTextView: TextView
        internal val iconView: ImageView

        init {
            itemView.isFocusable = true
            mainTextView = itemView.findViewById(R.id.luraSettingName)
            subTextView = itemView.findViewById(R.id.luraSettingValue)
            iconView = itemView.findViewById(R.id.luraSettingIcon)
        }
    }
}


