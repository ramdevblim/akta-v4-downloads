package com.akta.luraplayersampleapp.modern.events

import com.akta.luraplayer.api.enums.LuraScreenState

data class ScreenStateEvent(
    val state: LuraScreenState,
    val isVertical: Boolean = false,
)
