package com.akta.luraplayersampleapp.modern.custom

import androidx.recyclerview.widget.DiffUtil
import com.akta.luraplayersampleapp.modern.data.Video

class VideoDiffCallback(
    private val oldList: List<Video>,
    private val newList: List<Video>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Return whether items represent the same object
        return oldList[oldItemPosition] === newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Check whether old and new items' content are the same
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]

        return oldItem.config.lura?.assetId == newItem.config.lura?.assetId

    }
}