package com.akta.luraplayersampleapp.modern.data

import com.akta.luraplayer.api.configs.LuraConfiguration
import com.akta.luraplayer.api.configs.ads.LuraNonLinearConfiguration
import com.akta.luraplayer.api.configs.ads.NonLinearPaddingConfiguration
import com.akta.luraplayer.api.configs.ads.NonLinearPauseConfiguration
import com.akta.luraplayer.api.configs.ads.NonLinearPauseGenericConfiguration
import com.akta.luraplayer.api.configs.general.LuraXPosition
import com.akta.luraplayer.api.configs.general.LuraYPosition
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
    val adUrlForPause = "https://pubads.g.doubleclick.net/gampad/ads?sz=1920x1080&iu=%2F7009%2Fvix.tv%2Fviewer%2Fvod%2Fandroidtv%2Fondemand&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator=&url=https%3A%2F%2Fwww.vix.com&descriptionURL=https%3A%2F%2Fwww.vix.com%2Fdetail%2Fvideo-2875433&scor=1698&msid=com.univision.prendetv.dev&an=ViX&app_bundle=&rdid=193af088-8487-47f8-91be-4ec31b70d409&ditype=adid&is_lat=0&ppid=c5470848-6924-4936-baa0-388268822c18&hl=es&sid=63dd9bcd-4f0d-46dd-98a6-7b5df6416156&tfcd=0&rdp=0&user_agent=&cust_params=video_genres%253Ddrama%252Cromance%252Cclassic%252Cranchero%2526video_genres_first%253Ddrama%2526video_title%253Dlo%2520que%2520la%2520vida%2520me%2520robo%2520capitulo%25201%2526video_language%253Des%2526video_tags%253Dlo%2520que%2520la%2520vida%2520me%2520robo%2520capitulo%25201%252Clo%2520que%2520la%2520vida%2520me%2520robo%252Cangelique%2520boyer%252Csebastian%2520rulli%252Cluis%2520roberto%2520guzman%252Cosvaldo%2520benavides%252Cnovela%252Ctelevisa%252Ccapitulos%2520completos%2526video_id%253Dvideomcp2875433%2526video_content_vertical%253Dentertainment%2526video_supplier%253Dtelevisa%2526video_type%253Depisode%2526video_is_kids%253Dfalse%2526video_vod_type%253Davod%2526content_group%253Dfirst_party%252Cpromo%2526date_released%253D2022-02-14t140000000z%2526publish_window_mx_start%253D2021-12-22t050000000z%2526publish_window_mx_end%253D2030-12-31t050000000z%2526publish_window_es_start%253D2021-12-22t050000000z%2526publish_window_es_end%253D2030-12-31t050000000z%2526tr_id%253Drobo100000003%2526tms_id%253Dep035954540001%2526video_duration%253D2821%2526video_rating%253Dtv-14%2526video_sub_rating%253Dd%2526series_id%253Dseriesmcp654%2526series_title%253Dlo%2520que%2520la%2520vida%2520me%2520robo%2526series_episode_number%253D1%2526series_season_id%253Dseasonmcp654_1novelaseasonid1%2526series_season_title%253Depisodios%25201%2520-%252020%2526series_season_number%253D1%2526series_season_count%253D10%2526stream_type%253Dvod%2526is_epg%253Dfalse%2526navigation_section%253Dondemand%2526carousel_title%253Dnovelas%2520mas%2520vistas%2526install_age_days%253D0%2526countryGroup%253Dmx"
    return LuraConfiguration(
        name = config.name,
        lura = config.lura?.copy(),
        controls = config.controls,
        content = config.content?.copy(),
        ads = config.ads?.copy(
            nonLinear = LuraNonLinearConfiguration(
                pause = NonLinearPauseConfiguration(
                    provider = "generic",
                    generic = NonLinearPauseGenericConfiguration(
                        url = adUrlForPause,
                        xPosition = LuraXPosition.Center,
                        yPosition = LuraYPosition.Center,
                        backgroundOpacity = 0.4f,
                        padding = NonLinearPaddingConfiguration(
                            left = "1%",
                            right = "1%",
                            top = "1%",
                            bottom = "1%"
                        )
                    )
                )
            )
        ),
        plugins = config.plugins?.copy(conviva = conviva),
        cast = config.cast?.copy(),
        debug = config.debug
    )
}