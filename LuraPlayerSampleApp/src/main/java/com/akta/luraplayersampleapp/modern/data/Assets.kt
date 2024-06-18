package com.akta.luraplayersampleapp.modern.data

import kotlinx.serialization.Serializable

@Serializable
data class Assets(
    val groupTitle: String,
    val debugOnly: Boolean = false,
    var configs: List<Video>
)
