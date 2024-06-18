package com.akta.luraplayersampleapp.modern.events

import androidx.recyclerview.widget.RecyclerView

data class AssetRefreshEvent(
    val assetPosition: Int = RecyclerView.NO_POSITION,
)
