package com.akta.luraplayersampleapp.modern.events

data class AssetSelectedEvent(
    val assets: String,
    val selectedAssetPosition: Int
)
