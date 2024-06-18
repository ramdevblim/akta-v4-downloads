package com.akta.luraplayersampleapp.modern.data

import com.akta.luraplayer.api.configs.LuraConfiguration
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Video(
    val title: String? = null,
    val preview: String? = null,
    val debugOnly: Boolean = false,
    var config: LuraConfiguration,
)

fun Video.toLura(): LuraConfiguration {
    val conviva = config.plugins?.conviva?.copy(
        contentInfo = config.plugins?.conviva?.contentInfo?.toMutableMap()?.apply {
            put("SenderSessionId", UUID.randomUUID().toString())
        }
    )
    return LuraConfiguration(
        name = config.name,
        lura = config.lura?.copy(),
        controls = config.controls,
        content = config.content?.copy(),
        ads = config.ads?.copy(),
        plugins = config.plugins?.copy(conviva = conviva),
        cast = config.cast?.copy(),
        debug = config.debug
    )
}