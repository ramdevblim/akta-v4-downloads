package com.akta.luraplayercontrols.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.ui.LuraPopupType


internal class LuraSubMenuAdapter(
    internal val texts: List<String>,
    internal val values: List<Any>,
    internal val type: LuraPopupType? = null,
    private val onItemClick: (Any) -> Unit,
) :
    RecyclerView.Adapter<LuraSubMenuAdapter.SubMenuViewHolder>() {
    internal var selectedIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubMenuViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.lura_sub_menu_item_view, parent, false)
        return SubMenuViewHolder(v)
    }

    override fun onBindViewHolder(holder: SubMenuViewHolder, position: Int) {
        if (position < texts.size) {
            holder.textView.text = texts[position]
        }
        if (position == selectedIndex) {
            holder.itemView.isSelected = true
            holder.checkView.visibility = View.VISIBLE
        } else {
            holder.itemView.isSelected = false
            holder.checkView.visibility = View.INVISIBLE
        }
        holder.itemView.setOnClickListener { _: View? ->
            val absoluteAdapterPosition = holder.absoluteAdapterPosition
            onItemClick(values[absoluteAdapterPosition])
            selectedIndex = absoluteAdapterPosition
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return texts.size
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


