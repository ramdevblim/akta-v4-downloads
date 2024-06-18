package com.akta.luraplayercontrols.ui

import android.content.Context
import android.os.Build
import android.transition.Fade
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.akta.luraplayer.api.models.LuraTrack
import com.akta.luraplayer.api.models.LuraTracks
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.extensions.toBitrateName
import com.akta.luraplayercontrols.extensions.toLanguageName
import com.akta.luraplayercontrols.ui.adapters.LuraMenuAdapter
import com.akta.luraplayercontrols.ui.adapters.LuraSubMenuAdapter
import com.akta.luraplayercontrols.ui.adapters.LuraTrackAdapter
import com.akta.luraplayercontrols.utils.LuraControlsSharedPreferencesUtil

interface LuraPopupCallbacks {
    fun onSubtitleTap(track: LuraTrack.Text)
    fun onVideoTap(track: LuraTrack.Video)
    fun onAudioTap(track: LuraTrack.Audio)
    fun onPlaybackRateTap(playbackSpeed: Float)
    fun onBackClick(type: LuraPopupType?)
    fun onRemoveCallback()
    fun onCaptionUpdateCallback()
}

enum class LuraPopupType {
    Settings,
    Captions,
    Audio,
    CaptionsAndAudios,
    CaptionSettings,
    Font,
    FontSize,
    TextColor,
    TextOpacity,
    BackgroundColor,
    BackgroundOpacity,
    HighlightColor,
    HighlightOpacity,
    Capitalize
}

class LuraPopupWindow(
    private val context: Context,
    private val sharedPreferencesUtil: LuraControlsSharedPreferencesUtil,
    genericControlBar: LuraControlBar,
    private val callbacks: LuraPopupCallbacks,
) : PopupWindow(context) {
    private val menuView: LuraMenuView = LuraMenuView(context).apply {
        topContainer.setOnClickListener {
            if (adapters.isNotEmpty()) {
                val type = when (val adapter = adapters.removeLast()) {
                    is LuraMenuAdapter -> (adapter as? LuraMenuAdapter)?.type
                    is LuraSubMenuAdapter -> (adapter as? LuraSubMenuAdapter)?.type
                    else -> null
                }
                callbacks.onBackClick(type)
            } else {
                dismiss()
            }
        }
    }

    private val settingNames = context.resources.getStringArray(R.array.luraSettingNames)
    private val captionsAndAudiosNames =
        context.resources.getStringArray(R.array.luraCaptionAndAudioNames)
    private val settingIcons = arrayOf(
        ResourcesCompat.getDrawable(context.resources, R.drawable.playback, context.theme),
        ResourcesCompat.getDrawable(context.resources, R.drawable.subtitles, context.theme),
        ResourcesCompat.getDrawable(context.resources, R.drawable.quality, context.theme),
    )
    private val controlBar = genericControlBar as View

    private var playbackNames =
        context.resources.getStringArray(R.array.luraPlaybackSpeedNames).toList()
    private var playbackValues = listOf(0.25f, 0.50f, 0.75f, 1.00f, 1.25f, 1.50f, 1.70f, 2.00f)

    private val captionSettingsTitle =
        context.resources.getString(R.string.luraSettingSubtitlesTitle)
    private val captionSettingsNames = context.resources.getStringArray(R.array.captionNames)
    private val captionFontsNames = context.resources.getStringArray(R.array.captionFonts).toList()
    private val captionFontsValues = listOf(
        R.font.courier_prime,
        R.font.tinos,
        R.font.roboto_mono,
        R.font.inter,
        R.font.dekko,
        R.font.petit_formal_script,
        R.font.alegreya_sc,
        R.font.alegreya_sans_sc,
    )
    private val captionFontSizeNames =
        context.resources.getStringArray(R.array.captionFontSizes).toList()
    private val captionFontSizesValues =
        listOf(-1f, 12f, 16f, 20f, 24f, 28f, 32f, 36f, 40f, 44f, 48f, 52f, 56f, 60f)
    private val captionColorNames = context.resources.getStringArray(R.array.captionColors).toList()
    private val captionColorValues = listOf(
        R.color.white,
        R.color.turqoise,
        R.color.blue,
        R.color.green,
        R.color.yellow,
        R.color.magenta,
        R.color.red,
        R.color.black,
    )
    private val captionTextOpacityNames =
        context.resources.getStringArray(R.array.captionTextOpacities).toList()
    private val captionTextOpacityValues = listOf(50, 75, 100)

    private val captionBackgroundOpacityNames =
        context.resources.getStringArray(R.array.captionBackgroundOpacities).toList()
    private val captionBackgroundOpacityValues = listOf(0, 25, 50, 75, 100)

    private val captionHighlightOpacityNames =
        context.resources.getStringArray(R.array.captionHighlightOpacities).toList()
    private val captionHighlightOpacityValues = listOf(0, 25, 50, 75, 100)

    private val captionCapitalizeNames =
        context.resources.getStringArray(R.array.captionCapitalizes).toList()
    private val captionCapitalizeValues = listOf(true, false)

    init {
        contentView = menuView
        width = controlBar.width
        height = controlBar.height
        val bgColor = ResourcesCompat.getDrawable(
            context.resources,
            R.color.lura_menu_bg_color,
            context.theme
        )
        setBackgroundDrawable(bgColor)
        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            enterTransition = Fade()
            exitTransition = Fade()
        }
    }

    private val captionsAdapter: LuraTrackAdapter = LuraTrackAdapter(tracks = emptyList()) {
        callbacks.onSubtitleTap(it as LuraTrack.Text)
        dismiss()
    }

    private val audioAdapter: LuraTrackAdapter = LuraTrackAdapter(tracks = emptyList()) {
        callbacks.onAudioTap(it as LuraTrack.Audio)
        dismiss()
    }

    private val settingsAdapter: LuraMenuAdapter = LuraMenuAdapter(
        mainTexts = settingNames,
        iconIds = settingIcons,
        type = LuraPopupType.Settings
    ) { i ->
        onSettingItemClick(i)
    }

    private val captionAndAudioAdapter: LuraMenuAdapter = LuraMenuAdapter(
        mainTexts = captionsAndAudiosNames,
        iconIds = Array(2) { null },
        type = LuraPopupType.CaptionsAndAudios
    ) { i ->
        onCaptionAndAudioItemClick(i)
    }

    private val playbackSpeedAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = playbackNames,
        values = playbackValues
    ) {
        callbacks.onPlaybackRateTap(it as Float)
        dismiss()
    }

    private val captionSettingsAdapter: LuraMenuAdapter = LuraMenuAdapter(
        mainTexts = captionSettingsNames,
        iconIds = emptyArray(),
        type = LuraPopupType.CaptionSettings
    ) {
        onCaptionSettingsItemClick(it)
    }

    private val qualityAdapter: LuraTrackAdapter = LuraTrackAdapter(
        tracks = emptyList(),
    ) {
        callbacks.onVideoTap(it as LuraTrack.Video)
        dismiss()
    }

    private fun onSettingItemClick(i: Int) {
        val (adapter, selectedIndex) = when (i) {
            0 -> Pair(playbackSpeedAdapter, playbackSpeedAdapter.selectedIndex)
            1 -> Pair(captionSettingsAdapter, null)
            2 -> Pair(qualityAdapter, qualityAdapter.selectedIndex)
            else -> return
        }
        menuView.titleView.text = settingNames[i]
        menuView.addAdapter(adapter)
        displaySettingsWindow(adapter, selectedIndex)
        callbacks.onRemoveCallback()
    }

    private val captionFontsAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionFontsNames,
        values = captionFontsValues,
        type = LuraPopupType.Font
    ) {
        updateCaptionsAdapters(type = LuraPopupType.Font, it)
    }

    private val captionFontSizesAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionFontSizeNames,
        values = captionFontSizesValues,
        type = LuraPopupType.FontSize
    ) {
        updateCaptionsAdapters(type = LuraPopupType.FontSize, it)
    }

    private val captionTextColorsAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionColorNames,
        values = captionColorValues,
        type = LuraPopupType.TextColor
    ) {
        updateCaptionsAdapters(type = LuraPopupType.TextColor, it)
    }

    private val captionTextOpacitiesAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionTextOpacityNames,
        values = captionTextOpacityValues,
        type = LuraPopupType.TextOpacity
    ) {
        updateCaptionsAdapters(type = LuraPopupType.TextOpacity, it)
    }

    private val captionBackgroundColorsAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionColorNames,
        values = captionColorValues,
        type = LuraPopupType.BackgroundColor
    ) {
        updateCaptionsAdapters(type = LuraPopupType.BackgroundColor, it)
    }

    private val captionBackgroundOpacitiesAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionBackgroundOpacityNames,
        values = captionBackgroundOpacityValues,
        type = LuraPopupType.BackgroundOpacity
    ) {
        updateCaptionsAdapters(type = LuraPopupType.BackgroundOpacity, it)
    }

    private val captionHighlightColorsAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionColorNames,
        values = captionColorValues,
        type = LuraPopupType.HighlightColor
    ) {
        updateCaptionsAdapters(type = LuraPopupType.HighlightColor, it)
    }

    private val captionHighlightOpacitiesAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionHighlightOpacityNames,
        values = captionHighlightOpacityValues,
        type = LuraPopupType.HighlightOpacity
    ) {
        updateCaptionsAdapters(type = LuraPopupType.HighlightOpacity, it)
    }

    private val captionCapitalizesAdapter: LuraSubMenuAdapter = LuraSubMenuAdapter(
        texts = captionCapitalizeNames,
        values = captionCapitalizeValues,
        type = LuraPopupType.Capitalize
    ) {
        updateCaptionsAdapters(type = LuraPopupType.Capitalize, it)
    }

    override fun dismiss() {
        menuView.adapters.clear()
        super.dismiss()
    }

    private fun updateCaptionsAdapters(type: LuraPopupType, value: Any) {
        when (type) {
            LuraPopupType.Font ->
                sharedPreferencesUtil.captionsFont = value as Int

            LuraPopupType.FontSize ->
                sharedPreferencesUtil.captionsFontSize = value as Float

            LuraPopupType.TextColor ->
                sharedPreferencesUtil.captionsTextColor = value as Int

            LuraPopupType.TextOpacity ->
                sharedPreferencesUtil.captionsTextOpacity = value as Int

            LuraPopupType.BackgroundColor ->
                sharedPreferencesUtil.captionsBackgroundColor = value as Int

            LuraPopupType.BackgroundOpacity ->
                sharedPreferencesUtil.captionsBackgroundOpacity = value as Int

            LuraPopupType.HighlightColor ->
                sharedPreferencesUtil.captionsHighlightColor = value as Int

            LuraPopupType.HighlightOpacity ->
                sharedPreferencesUtil.captionsHighlightOpacity = value as Int

            LuraPopupType.Capitalize ->
                sharedPreferencesUtil.captionsCapitalize = value as Boolean

            else -> {}
        }
        updateCaptionSettingsAdapters()
        callbacks.onCaptionUpdateCallback()
    }


    private fun onCaptionSettingsItemClick(i: Int) {
        val (adapter, selectedIndex) = when (i) {
            0 -> Pair(captionFontsAdapter, captionFontsAdapter.selectedIndex)
            1 -> Pair(captionFontSizesAdapter, captionFontSizesAdapter.selectedIndex)
            2 -> Pair(captionTextColorsAdapter, captionTextColorsAdapter.selectedIndex)
            3 -> Pair(captionTextOpacitiesAdapter, captionTextOpacitiesAdapter.selectedIndex)
            4 -> Pair(captionBackgroundColorsAdapter, captionBackgroundColorsAdapter.selectedIndex)
            5 -> Pair(
                captionBackgroundOpacitiesAdapter,
                captionBackgroundOpacitiesAdapter.selectedIndex
            )

            6 -> Pair(captionHighlightColorsAdapter, captionHighlightColorsAdapter.selectedIndex)
            7 -> Pair(
                captionHighlightOpacitiesAdapter,
                captionHighlightOpacitiesAdapter.selectedIndex
            )

            8 -> Pair(captionCapitalizesAdapter, captionCapitalizesAdapter.selectedIndex)
            else -> return
        }
        menuView.titleView.text = captionSettingsNames[i]
        menuView.addAdapter(adapter)
        displaySettingsWindow(adapter, selectedIndex)
        callbacks.onRemoveCallback()
    }


    private fun onCaptionAndAudioItemClick(i: Int) {
        val (adapter, selectedIndex) = when (i) {
            0 -> Pair(captionsAdapter, captionsAdapter.selectedIndex)
            1 -> Pair(audioAdapter, audioAdapter.selectedIndex)
            else -> return
        }
        menuView.titleView.text = captionsAndAudiosNames[i]
        displaySettingsWindow(adapter, selectedIndex)
        callbacks.onRemoveCallback()
    }

    private fun displaySettingsWindow(
        adapter: RecyclerView.Adapter<*>,
        scrollPosition: Int? = null,
    ) {
        menuView.updateAdapter(adapter)
        if (scrollPosition != null) {
            menuView.recyclerView.scrollToPosition(scrollPosition)
        }
        updateWindowSize()
    }

    private fun updateWindowSize() {
        width = controlBar.width
        height = controlBar.height
        val i = IntArray(2)
        controlBar.getLocationInWindow(i)
        val (xOff, yOff) = Pair(i[0], i[1])
        showAtLocation(controlBar, Gravity.TOP, xOff, yOff)
    }

    internal fun updateAdapters(
        tracks: LuraTracks? = null,
        speed: Float?,
        isLive: Boolean? = false,
    ) {
        updateSpeedAdapter(speed, isLive ?: false)
        updateTrackAdapters(tracks)
        updateCaptionSettingsAdapters()
    }

    private fun updateTrackAdapters(tracks: LuraTracks?) {
        if (tracks != null) {
            updateCaptionsAdapter(tracks)
            updateQualityAdapter(tracks)
            updateAudioAdapter(tracks)
        } else {
            captionsAdapter.tracks = emptyList()
            audioAdapter.tracks = emptyList()
            qualityAdapter.tracks = emptyList()
            settingsAdapter.setSubTextAtPosition(1, "")
            captionAndAudioAdapter.setSubTextAtPosition(0, "")
            settingsAdapter.setSubTextAtPosition(2, "")
            captionAndAudioAdapter.setSubTextAtPosition(1, "")
        }
    }

    private fun updateQualityAdapter(tracks: LuraTracks) {
        val firstGroups = tracks.video.filter { it.active }
        val firstGroup = firstGroups.firstOrNull { it.isCurrent }
        val isFirstAdaptiveSupported = firstGroup?.isAdaptiveSupported
        val autoVideoTrack = if (firstGroups.size > 1 && isFirstAdaptiveSupported == true)
            LuraTrack.Video(
                bitrate = firstGroup.bitrate,
                isAdaptiveSupported = true,
                active = firstGroup.active
            )
        else if (isFirstAdaptiveSupported == true) LuraTrack.Video(
            isAdaptiveSupported = true
        )
        else null
        val videos = autoVideoTrack?.let {
            tracks.video
                .sortedBy { video -> video.bitrate }
                .plus(it)
        } ?: tracks.video
        val (selected, index) = if (firstGroups.size > 1) {
            Pair(videos.lastOrNull { it.index == -1 }, videos.lastIndex)
        } else {
            val video = videos.firstOrNull { it.active }
            val index = try {
                videos.indexOf(video)
            } catch (_: Exception) {
                -1
            }
            Pair(video, index)
        }
        qualityAdapter.tracks = videos
        qualityAdapter.selectedIndex = index
        settingsAdapter.setSubTextAtPosition(
            2,
            selected?.toBitrateName(context = context) ?: ""
        )
    }

    private fun updateAudioAdapter(tracks: LuraTracks) {
        val audios = tracks.audio
        val (selected, index) = Pair(
            audios.firstOrNull { it.active },
            audios.indexOfFirst { it.active }
        )
        captionAndAudioAdapter.setSubTextAtPosition(
            1,
            (selected?.toLanguageName(context = context) ?: "")
        )
        audioAdapter.tracks = audios
        audioAdapter.selectedIndex = index
    }

    private fun updateCaptionsAdapter(tracks: LuraTracks) {
        val texts = if (tracks.caption.isNotEmpty()) {
            listOf(LuraTrack.Text()) + tracks.caption
        } else tracks.caption
        val firstSelectedIndex = texts.indexOfFirst { it.active }
        val (selected, index) = Pair(
            (texts.firstOrNull { it.active } ?: texts.firstOrNull()),
            if (firstSelectedIndex == -1) 0 else firstSelectedIndex
        )
        captionAndAudioAdapter.setSubTextAtPosition(
            0,
            selected?.toLanguageName(context = context) ?: ""
        )
        captionsAdapter.tracks = texts
        captionsAdapter.selectedIndex = index
    }

    private fun updateSpeedAdapter(speed: Float?, isLive: Boolean = false) {
        if (speed != null) {
            if (isLive) {
                settingsAdapter.setSubTextAtPosition(0, "")
            } else {
                playbackSpeedAdapter.selectedIndex = try {
                    playbackSpeedAdapter.values.indexOf(speed)
                } catch (_: Exception) {
                    -1
                }
                settingsAdapter.setSubTextAtPosition(
                    0,
                    playbackSpeedAdapter.texts.getOrNull(playbackSpeedAdapter.selectedIndex) ?: ""
                )
            }
        }
    }

    private fun updateCaptionSettingsAdapters() {
        captionFontsAdapter.selectedIndex =
            captionFontsValues.indexOf(sharedPreferencesUtil.captionsFont)
        captionFontSizesAdapter.selectedIndex =
            captionFontSizesValues.indexOf(sharedPreferencesUtil.captionsFontSize)
        captionTextColorsAdapter.selectedIndex =
            captionColorValues.indexOf(sharedPreferencesUtil.captionsTextColor)
        captionTextOpacitiesAdapter.selectedIndex =
            captionTextOpacityValues.indexOf(sharedPreferencesUtil.captionsTextOpacity)
        captionBackgroundColorsAdapter.selectedIndex =
            captionColorValues.indexOf(sharedPreferencesUtil.captionsBackgroundColor)
        captionBackgroundOpacitiesAdapter.selectedIndex =
            captionBackgroundOpacityValues.indexOf(sharedPreferencesUtil.captionsBackgroundOpacity)
        captionHighlightColorsAdapter.selectedIndex =
            captionColorValues.indexOf(sharedPreferencesUtil.captionsHighlightColor)
        captionHighlightOpacitiesAdapter.selectedIndex =
            captionHighlightOpacityValues.indexOf(sharedPreferencesUtil.captionsHighlightOpacity)
        captionCapitalizesAdapter.selectedIndex =
            captionCapitalizeValues.indexOf(sharedPreferencesUtil.captionsCapitalize)
        menuView.adapters.forEach {
            it.notifyDataSetChanged()
        }
    }


    internal fun updateSize() {
        val i = IntArray(2)
        controlBar.getLocationInWindow(i)
        val (xOff, yOff) = Pair(i[0], i[1])
        val (w, h) = Pair(controlBar.width, controlBar.height)
        update(xOff, yOff, w, h)
    }

    internal fun show(type: LuraPopupType) {
        when (type) {
            LuraPopupType.Settings -> {
                menuView.adapters.clear()
                menuView.titleView.text = ""
                displaySettingsWindow(settingsAdapter)
            }

            LuraPopupType.CaptionsAndAudios -> displaySettingsWindow(captionAndAudioAdapter)
            LuraPopupType.Captions -> displaySettingsWindow(
                captionsAdapter,
                captionsAdapter.selectedIndex
            )

            LuraPopupType.Audio -> displaySettingsWindow(
                audioAdapter,
                audioAdapter.selectedIndex
            )

            LuraPopupType.Font,
            LuraPopupType.FontSize,
            LuraPopupType.TextColor,
            LuraPopupType.TextOpacity,
            LuraPopupType.BackgroundColor,
            LuraPopupType.BackgroundOpacity,
            LuraPopupType.HighlightColor,
            LuraPopupType.HighlightOpacity,
            LuraPopupType.Capitalize -> {
                menuView.titleView.text = captionSettingsTitle
                val adapter = menuView.adapters.last() as LuraMenuAdapter
                displaySettingsWindow(adapter)
            }

            LuraPopupType.CaptionSettings -> {
                menuView.titleView.text = ""
                displaySettingsWindow(settingsAdapter)
            }
        }
    }
}