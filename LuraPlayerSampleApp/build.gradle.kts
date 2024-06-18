@file:Suppress("UnstableApiUsage")

import java.io.ByteArrayOutputStream
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlinx-serialization")
}

val keystorePropertiesFile = project.file("../keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

fun getGitHash(): String {
    val outputStream = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = outputStream
    }

    return "\"" + outputStream.toString().trim() + "\""
}

android {
    namespace = "com.akta.luraplayersampleapp"
    buildToolsVersion = "34.0.0"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.akta.luraplayersampleapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "4.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            //buildConfigField("String", "GIT_HASH", "")
            isMinifyEnabled = true
        }
        getByName("debug") {
            //buildConfigField("String", "GIT_HASH", "'master'")
        }
    }
    compileOptions {
        sourceCompatibility(11)
        targetCompatibility(11)
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.aar"))))
    implementation(files("../luraplayercontrols/libs/LuraPlayer.aar"))
    implementation(project(":LuraPlayerControls"))

    //Android
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${com.akta.aktaplayer.LuraVersions.LIFECYCLE}")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha13")
    implementation("androidx.appcompat:appcompat:${com.akta.aktaplayer.LuraVersions.APP_COMPAT}")
    implementation("com.google.android.material:material:${com.akta.aktaplayer.LuraVersions.MATERIAL}")
    implementation("org.greenrobot:eventbus:3.3.1")

    //Kotlin
    implementation("androidx.core:core-ktx:${com.akta.aktaplayer.LuraVersions.CORE_KTX}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${com.akta.aktaplayer.LuraVersions.KOTLIN}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${com.akta.aktaplayer.LuraVersions.KOTLIN}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${com.akta.aktaplayer.LuraVersions.KOTLIN_COROUTINES}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${com.akta.aktaplayer.LuraVersions.KOTLIN_SERIALIZATION}")

    //Media3
    implementation("androidx.media3:media3-exoplayer:${com.akta.aktaplayer.LuraVersions.MEDIA_3}")
    implementation("androidx.media3:media3-exoplayer-dash:${com.akta.aktaplayer.LuraVersions.MEDIA_3}")
    implementation("androidx.media3:media3-exoplayer-hls:${com.akta.aktaplayer.LuraVersions.MEDIA_3}")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:${com.akta.aktaplayer.LuraVersions.MEDIA_3}")
    implementation("androidx.media3:media3-ui:${com.akta.aktaplayer.LuraVersions.MEDIA_3}")

    //GAM
    implementation("androidx.media3:media3-exoplayer-ima:${com.akta.aktaplayer.LuraVersions.MEDIA_3}")

    //Plugins
    implementation("com.google.android.gms:play-services-pal:${com.akta.aktaplayer.LuraVersions.PAL}")
    implementation("com.comscore:android-analytics:${com.akta.aktaplayer.LuraVersions.COMSCORE}")
    implementation("com.conviva.sdk:conviva-core-sdk:${com.akta.aktaplayer.LuraVersions.CONVIVA}")

    //Adobe Experience
    implementation("com.adobe.marketing.mobile:assurance:${com.akta.aktaplayer.LuraVersions.ADOBE.ASSURANCE}")
    implementation("com.adobe.marketing.mobile:media:${com.akta.aktaplayer.LuraVersions.ADOBE.MEDIA}")
    implementation("com.adobe.marketing.mobile:core:${com.akta.aktaplayer.LuraVersions.ADOBE.SDK_CORE}")
    implementation("com.adobe.marketing.mobile:analytics:${com.akta.aktaplayer.LuraVersions.ADOBE.ANALYTICS}")
    implementation("com.adobe.marketing.mobile:identity:${com.akta.aktaplayer.LuraVersions.ADOBE.IDENTITY}")

    //Chromecast
    implementation("androidx.mediarouter:mediarouter:${com.akta.aktaplayer.LuraVersions.MEDIA_ROUTER}")
    implementation("com.google.android.gms:play-services-cast-framework:${com.akta.aktaplayer.LuraVersions.CHROMECAST}")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}