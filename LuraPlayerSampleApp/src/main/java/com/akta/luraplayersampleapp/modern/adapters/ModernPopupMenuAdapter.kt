package com.akta.luraplayersampleapp.modern.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayersampleapp.R

class ModernPopupMenuAdapter(
    private val context: Context,
    private val menuItems: List<Pair<String, Drawable>>,
    private val onItemClick: (String) -> Unit,
) : RecyclerView.Adapter<ModernPopupMenuAdapter.ModernPopupMenuItemViewHolder>() {

    class ModernPopupMenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView
        val icon: ImageView

        init {
            title = itemView.findViewById(R.id.option_title)
            icon = itemView.findViewById(R.id.option_icon)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModernPopupMenuItemViewHolder {
        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.modern_popup_menu_item, parent, false)
        return ModernPopupMenuItemViewHolder(itemView)
    }

    override fun onBindViewHolder(
        holder: ModernPopupMenuItemViewHolder,
        @SuppressLint("RecyclerView") position: Int,
    ) {
        val item = menuItems[position]
        holder.title.text = item.first
        holder.icon.setImageDrawable(item.second)
        holder.icon.setColorFilter(Color.argb(255, 255, 255, 255))
        holder.itemView.setOnClickListener { onItemClick(item.first) }
    }

    override fun getItemCount(): Int = menuItems.size

}