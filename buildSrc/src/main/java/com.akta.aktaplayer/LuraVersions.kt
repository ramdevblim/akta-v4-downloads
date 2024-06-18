package com.akta.aktaplayer

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.project

object LuraVersions {
    const val MEDIA_3 = "1.3.1"

    const val PAL = "20.2.0"
    const val CONVIVA = "4.0.35"
    const val COMSCORE = "6.9.3"
    const val NIELSEN = "9.1.0.0"
    val ADOBE = AdobeVersions

    const val LIFECYCLE = "2.6.1"
    const val APP_COMPAT = "1.6.1"
    const val MATERIAL = "1.8.0"
    const val CONSTRAINT_LAYOUT = "2.1.4"

    const val CORE_KTX = "1.10.0"
    const val KOTLIN = "1.8.20"
    const val KOTLIN_COROUTINES = "1.6.4"
    const val KOTLIN_SERIALIZATION = "1.5.0"

    const val MEDIA_ROUTER = "1.4.0"
    const val CHROMECAST = "21.3.0"
}

object AdobeVersions {
    const val ASSURANCE = "3.0.0"
    const val MEDIA = "3.1.0"
    const val SDK_CORE = "3.0.0"
    const val ANALYTICS = "3.0.0"
    const val IDENTITY = "3.0.0"
}