package com.akta.luraplayersampleapp.modern.custom

import com.akta.luraplayer.api.offline.LuraOfflineVideo
import java.text.CharacterIterator
import java.text.StringCharacterIterator

fun Long.asHumanReadableBytesUnits(): String {
    var innerByte = this
    if (innerByte == -1L) return "B"
    if (innerByte in -999..999) {
        return "B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (innerByte <= -999950 || innerByte >= 999950) {
        innerByte /= 1000
        ci.next()
    }
    return String.format("%cB", ci.current())
}

fun Long.asHumanReadableBytes(): String {
    var innerByte = this
    if (this == -1L) return "0 B"
    if (innerByte in -999..999) {
        return "$innerByte B"
    }
    val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
    while (innerByte <= -999950 || innerByte >= 999950) {
        innerByte /= 1000
        ci.next()
    }
    return String.format("%.1f %cB", innerByte / 1000.0, ci.current())
}

fun Long.asHumanReadableBytesWithoutInfo(): String {
    var innerByte = this
    if (this == -1L) return "0"
    if (innerByte in -999..999) {
        return "$innerByte"
    }
    while (innerByte <= -999950 || innerByte >= 999950) {
        innerByte /= 1000
    }
    return String.format("%.1f", innerByte / 1000.0)
}

fun List<LuraOfflineVideo>.calculateTotalSize(): Long {
    var size = 0L

    this.forEach {
        size += it.bytes
    }

    return size
}

fun Long?.toHourMinuteSecond(): String {
    if (this == null) return "00:00:00"

    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}