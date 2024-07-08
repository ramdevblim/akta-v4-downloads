package com.akta.luraplayercontrols.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.contains
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.mediarouter.app.MediaRouteButton
import com.akta.luraplayer.api.LuraPlayer
import com.akta.luraplayer.api.data.ad.LuraAd
import com.akta.luraplayer.api.data.ad.LuraAdBreak
import com.akta.luraplayer.api.data.ad.LuraAdIcon
import com.akta.luraplayer.api.enums.LuraCastTargets
import com.akta.luraplayer.api.event.LuraEvent
import com.akta.luraplayer.api.event.LuraEventData
import com.akta.luraplayer.api.event.LuraEventType
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.extensions.toMs
import com.akta.luraplayercontrols.extensions.toSecondsString
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@SuppressLint("InflateParams")
class LuraAdControlView : ConstraintLayout {
    private var player: LuraPlayer? = null

    private var showAlways: Boolean = false
    private val hideRunnable = Runnable { hideControls() }
    private val hideDelayMs = 2000L

    internal var currentAdBreak: LuraAdBreak? = null
    internal var currentAd: LuraAd? = null

    private val interactionLayout: ConstraintLayout
    private val controlsLayout: LinearLayout

    private val playPauseButton: LuraImageButton
    internal val muteUnmuteButton: LuraImageButton
    private val titleView: TextView
    private val timeView: TextView
    private val progressView: ProgressBar
    private val adSkipButton: LuraAdSkipButton
    private val adCountInfo: TextView
    internal val fullscreenButton: LuraImageButton

    private val topGuideline: Guideline
    private val bottomGuideline: Guideline

    internal var onPiPTap: (() -> Unit)? = null

    private var isShowingControls = true

    private val onPlayPauseClickListener: OnClickListener = OnClickListener {
        when (it.contentDescription) {
            resources.getString(R.string.pause) -> player?.pause()
            resources.getString(R.string.play) -> player?.play()
            resources.getString(R.string.replay) -> player?.play()
            else -> {}
        }
    }
    private val adUICastButton: MediaRouteButton = MediaRouteButton(context).apply {
        id = View.generateViewId()
        setAlwaysVisible(true)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        View.inflate(context, R.layout.lura_ad_view, this)
        setOnClickListener {
            if (isShowingControls) {
                //player?.clickAd()
            } else {
                showControls()
            }

            isShowingControls = !isShowingControls
        }
        interactionLayout = findViewById(R.id.interaction_layout)
        controlsLayout = findViewById(R.id.controls_layout)
        playPauseButton = findViewById(R.id.adPlayPauseView)
        muteUnmuteButton = findViewById(R.id.adMuteUnmuteView)
        titleView = findViewById(R.id.adTitleView)
        timeView = findViewById(R.id.adTimeView)
        progressView = findViewById(R.id.adProgressView)
        adSkipButton = findViewById(R.id.skip_button)
        adCountInfo = findViewById(R.id.ad_count)
        fullscreenButton = findViewById(R.id.adFullscreenView)
        topGuideline = findViewById(R.id.top_background_guideline)
        bottomGuideline = findViewById(R.id.bottom_background_guideline)
        adSkipButton.isEnabled = false
        adSkipButton.setOnClickListener { player?.skipAd() }
        playPauseButton.setOnClickListener(onPlayPauseClickListener)
    }

    private fun resetSkipButton() {
        adSkipButton.apply {
            isEnabled = false
            visibility = GONE
            text = ""
        }
    }

    private fun addEventHandler() {
        player?.addListener(Dispatchers.Main) { event ->
            when (event.type) {
                LuraEventType.LOADED_MEDIA_INFO -> resetSkipButton()

                LuraEventType.ERROR,
                LuraEventType.ENDED,
                -> visibility = View.GONE

                LuraEventType.AD_BREAK_STARTED -> {
                    currentAdBreak = event.data as? LuraAdBreak
                }

                LuraEventType.AD_STARTED -> {
                    currentAd = event.data as? LuraAd
                    val v = if (currentAdBreak?.stitcher != "GAM") VISIBLE
                    else GONE
                    if (currentAd?.interactiveFiles?.firstOrNull {
                            it.apiFramework?.equals("simid", true) != false
                        } == null) {
                        visibility = v
                    }
                    if (currentAd?.interactiveFiles?.firstOrNull { it.isSimid } != null) {
                        if (player?.isPlaying == true) {
                            this.visibility = View.GONE
                        }
                    }
                    if (currentAdBreak?.stitcher == "GAM") this.visibility = GONE
                }

                LuraEventType.AD_COMPLETED -> setAdCompleted()
                LuraEventType.AD_BREAK_COMPLETED -> setAdBreakCompleted()
                LuraEventType.AD_SKIPPED -> {
                    resetSkipButton()
                    setAdCompleted()
                    if (currentAdBreak?.stitcher != "GAM") {
                        setAdBreakCompleted()
                    }
                }

                LuraEventType.PAUSED -> {
                    removeCallbacks(hideRunnable)
                    showControls(true)
                }

                LuraEventType.INTERSTITIALS -> checkCurrentAdToUpdate(event)

                LuraEventType.PLAYING -> {
                    removeCallbacks(hideRunnable)
                    postDelayed(hideRunnable, hideDelayMs)
                }

                LuraEventType.CASTING_REQUESTED -> {
                    this.showAlways = true
                    showControls()
                }

                else -> {}
            }
            when (val data = event.data) {
                is LuraEventData.TimeUpdate -> setTime(data)
                else -> {}
            }
        }
    }

    private fun checkCurrentAdToUpdate(event: LuraEvent) {
        val currentAd = this.currentAd
        if (currentAd != null && currentAd.icons.isNullOrEmpty()) {
            val data = (event.data as? LuraEventData.InterstitialsData)?.breaks
            val adBreak = data?.firstOrNull { it == currentAdBreak }
            val ad = adBreak?.ads?.firstOrNull { it == currentAd }

            if (adBreak != null) {
                currentAdBreak = adBreak
            }
            if (ad != null) {
                this.currentAd = ad
                if (!ad.icons.isNullOrEmpty()) {
                    ad.icons?.forEach { addIconView(it) }
                }
            }
        }
    }

    private fun hideControls() {
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(this)

        newConstraintSet.clear(controlsLayout.id, ConstraintSet.BOTTOM)
        newConstraintSet.connect(
            controlsLayout.id,
            ConstraintSet.TOP,
            this.id,
            ConstraintSet.BOTTOM
        )

        newConstraintSet.clear(titleView.id, ConstraintSet.TOP)
        newConstraintSet.connect(
            titleView.id,
            ConstraintSet.BOTTOM,
            this.id,
            ConstraintSet.TOP
        )

        newConstraintSet.setGuidelinePercent(topGuideline.id, 0F)
        newConstraintSet.setGuidelinePercent(bottomGuideline.id, 1F)

        TransitionManager.beginDelayedTransition(this)
        newConstraintSet.applyTo(this)

        isShowingControls = false
    }

    private fun showControls(overrideHide: Boolean = false) {
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(this)

        newConstraintSet.clear(controlsLayout.id, ConstraintSet.TOP)
        newConstraintSet.connect(
            controlsLayout.id,
            ConstraintSet.BOTTOM,
            this.id,
            ConstraintSet.BOTTOM
        )

        newConstraintSet.clear(titleView.id, ConstraintSet.BOTTOM)
        newConstraintSet.connect(
            titleView.id,
            ConstraintSet.TOP,
            this.id,
            ConstraintSet.TOP
        )

        newConstraintSet.setGuidelinePercent(topGuideline.id, 0.35F)
        newConstraintSet.setGuidelinePercent(bottomGuideline.id, 0.65F)

        TransitionManager.beginDelayedTransition(this)
        newConstraintSet.applyTo(this)

        if (!showAlways && !overrideHide)
            postDelayed(hideRunnable, hideDelayMs)
    }

    fun setAdStart(luraAd: LuraAd?) {
        removeIconView()
        val ad = luraAd ?: return
        val adBreak = currentAdBreak ?: return
        if (adBreak.stitcher != "GAM") {
            if (ad.interactiveFiles?.none { it.isSimid } == true) {
                ad.icons?.forEach { addIconView(it) }
            }
            val dur = ad.duration.roundToLong().toSecondsString()
            setAdTimeAndProgress(
                adPos = 1,
                totalAds = adBreak.totalAds,
                pos = "00:00",
                dur = dur,
                adPosition = 0,
                duration = ad.duration.toMs()
            )
        }
    }

    private val adIconIds = mutableListOf<View>()

    @SuppressLint("ClickableViewAccessibility")
    private fun addIconView(luraIcon: LuraAdIcon) {
        if (luraIcon.data == null) return
        val view = findViewById<View>(luraIcon.hashCode())
        if (view != null) {
            return
        }
        val w = luraIcon.width?.toIntOrNull()?.toPx() ?: return
        val h = luraIcon.height?.toIntOrNull()?.toPx() ?: return

        val webView = WebView(this.context).apply {
            id = luraIcon.hashCode()
            visibility = VISIBLE
            setBackgroundColor(Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                isNestedScrollingEnabled = false
                javaScriptCanOpenWindowsAutomatically = false
                loadWithOverviewMode = true
                useWideViewPort = luraIcon.data?.contains("svg") == false
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            isScrollContainer = false
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(w, h).apply {
                setMargins(
                    interactionLayout.marginStart,
                    interactionLayout.marginBottom,
                    interactionLayout.marginEnd,
                    interactionLayout.marginBottom
                )
            }
        }
        addView(webView, this.childCount)
        adIconIds.add(webView)
        webView.loadUrl(luraIcon.data ?: "")
        val set = ConstraintSet()
        set.clone(this)
        when (luraIcon.yPosition) {
            "bottom" -> {
                set.connect(
                    webView.id,
                    ConstraintSet.BOTTOM,
                    this.interactionLayout.id,
                    ConstraintSet.TOP
                )
            }

            "top" -> {
                set.connect(
                    webView.id,
                    ConstraintSet.TOP,
                    this.id,
                    ConstraintSet.TOP
                )
            }

            else -> {
                val position = luraIcon.yPosition?.toIntOrNull()?.toPx() ?: 10
                val constraintSet = if (position < height / 2) {
                    ConstraintSet.TOP
                } else {
                    ConstraintSet.BOTTOM
                }
                set.setMargin(webView.id, LayoutParams.TOP, position)
                set.connect(webView.id, constraintSet, this.id, constraintSet)
            }

        }
        when (luraIcon.xPosition) {
            "left" -> {
                set.connect(
                    webView.id,
                    ConstraintSet.START,
                    this.id,
                    ConstraintSet.START
                )
            }

            "right" -> {
                set.connect(
                    webView.id,
                    ConstraintSet.END,
                    this.id,
                    ConstraintSet.END
                )
            }

            else -> {
                val position = luraIcon.xPosition?.toIntOrNull()?.toPx() ?: 10
                val constraintSet = if (position < width / 2) {
                    ConstraintSet.START
                } else {
                    ConstraintSet.END
                }
                set.setMargin(webView.id, LayoutParams.START, position)
                set.connect(webView.id, constraintSet, this.id, constraintSet)
            }
        }
        set.applyTo(this)
        webView.visibility = VISIBLE
        webView.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                val clickThrough = luraIcon.clicks?.clickThrough
                luraIcon.clicks?.clickTracking?.mapNotNull { it.url }?.forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        execute(it)
                    }
                }
                openClickThrough(clickThrough)
            }

            false
        }
    }

    private fun execute(
        apiUrl: String?,
        httpBody: String? = null,
        timeout: Int = 5000,
    ): String? {
        if (apiUrl.isNullOrBlank()) {
            return null
        }
        return try {
            val url = URL(apiUrl)
            return (url.openConnection() as HttpURLConnection).run {
                requestMethod = "GET"
                connectTimeout = timeout
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")

                if (httpBody != null) {
                    doOutput = true
                    outputStream.write(httpBody.toByteArray())
                }
                if (responseCode in 200..300) {
                    val response = inputStream.bufferedReader().readText()
                    response
                } else {
                    val error = errorStream.bufferedReader().readText()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun openClickThrough(clickThrough: String?) {
        if (clickThrough != null) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(clickThrough))
            startActivity(context, browserIntent, null)
        }
    }

    private fun Int.toPx(): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        resources.displayMetrics
    ).roundToInt()

    private fun removeIconView() {
        adIconIds.forEach {
            if (findViewById<View>(it.id) != null) {
                removeView(it)
                if (it is WebView) {
                    it.destroy()
                }
            }
        }
        adIconIds.clear()
    }

    private fun setAdCompleted() {
        currentAd = null
        removeIconView()
        progressView.progress = 0
        progressView.max = 0
        adSkipButton.text = ""
        adSkipButton.isEnabled = false
        adSkipButton.visibility = GONE
        visibility = View.GONE
    }

    private fun setAdBreakCompleted() {
        currentAdBreak = null
        visibility = View.GONE
    }

    fun setTime(event: LuraEventData.TimeUpdate) {
        if (currentAdBreak != null && visibility == View.INVISIBLE && currentAdBreak?.stitcher?.contains("GAM", ignoreCase = true) == false) {
            if (currentAd?.interactiveFiles?.firstOrNull {
                    it.apiFramework?.equals("simid", true) == true
                } == null)
                visibility = View.VISIBLE
        }
        val ad = currentAd
        val isLive = player?.isLive ?: return
        val isCasting = player?.isCasting(LuraCastTargets.CHROMECAST) == true
        if (event.ad != null) {
            val adPos = event.ad?.adPosition ?: 0
            val totalAds = event.ad?.totalAds ?: 0
            val (adPosition, duration) = if (isCasting) {
                handleAdForVOD(event)
            } else {
                if (!isLive || currentAdBreak?.origin.equals(
                        "CLIENTSIDE",
                        ignoreCase = true
                    )
                ) handleAdForVOD(event)
                else Pair(0L, 0L)
            }
            val pos = (adPosition / 1000.0).roundToLong().toSecondsString()
            val dur = (duration / 1000.0).roundToLong().toSecondsString()
            setAdTimeAndProgress(adPos, totalAds, pos, dur, adPosition, duration)
        }
        if (ad != null) {
            if (currentAd?.interactiveFiles?.none { it.isSimid } == true) {
                currentAd?.icons?.forEach { addIconView(it) }
            }
        }
    }

    private fun setAdTimeAndProgress(
        adPos: Int,
        totalAds: Int,
        pos: String?,
        dur: String?,
        adPosition: Long,
        duration: Long,
    ) {
        if (player?.isLive == true && currentAdBreak?.origin.equals(
                "SERVERSIDE",
                ignoreCase = true
            )
        ) {
            interactionLayout.visibility = View.GONE
            progressView.visibility = View.INVISIBLE

            timeView.text = resources.getString(R.string.luraLive)
            timeView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.lura_live_ad_indicator,
                0,
                0,
                0
            )
        } else {
            interactionLayout.visibility = View.VISIBLE
            progressView.visibility = View.VISIBLE

            timeView.text = resources.getString(R.string.lura_video_time, pos, dur)
            timeView.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0
            )
            progressView.progress = adPosition.toInt()
            progressView.max = duration.toInt()
            adCountInfo.text = resources.getString(R.string.luraAdCountInfo, adPos, totalAds)
        }
    }

    private fun handleAdForVOD(
        event: LuraEventData.TimeUpdate,
    ): Pair<Long, Long> {
        val adData = event.ad ?: return Pair(0L, 0L)
        val duration = adData.adDurationMs
        val adPosition = adData.timeInAdMs
        val skipOffset = adData.skipOffsetMs
        if (skipOffset > 0) {
            adSkipButton.apply {
                visibility = VISIBLE
                isEnabled = adPosition > skipOffset
                text = if (adPosition > skipOffset) {
                    if (!isFocused)
                        requestFocus()
                    context.getString(R.string.luraSkipAd)
                } else {
                    context.getString(
                        R.string.luraCanSkipAdAfterSecond,
                        adData.timeToSkip.toString()
                    )
                }
            }
        } else {
            adSkipButton.apply {
                visibility = GONE
                text = ""
            }
        }
        return Pair(adPosition, duration)
    }

    internal fun setReplay() {
        playPauseButton.apply {
            setImageResource(R.drawable.replay)
            contentDescription = resources.getString(R.string.replay)
        }
    }

    internal fun setIsPlaying(isPlaying: Boolean) {
        playPauseButton.apply {
            contentDescription = if (isPlaying) {
                setImageResource(R.drawable.pause)
                resources.getString(R.string.pause)
            } else {
                setImageResource(R.drawable.play)
                resources.getString(R.string.play)
            }
        }
    }

    internal fun setSkipPreview(time: Int) {
        player?.requestTrickPlayImage(time.toLong())?.let { adSkipButton.preview = it }
    }

    internal fun setSkipPreview(bitmap: Bitmap?) {
        if (bitmap != null)
            adSkipButton.preview = bitmap
    }

    internal fun addChromecastButton() {
        if (!controlsLayout.contains(adUICastButton)) {
            CastButtonFactory.setUpMediaRouteButton(context, adUICastButton)
            controlsLayout.addView(adUICastButton, 3)
        }
    }

    internal fun removeChromecastButton() {
        if (controlsLayout.contains(adUICastButton)) {
            controlsLayout.removeView(adUICastButton)
        }
    }

    fun addPlayer(luraPlayer: LuraPlayer?) {
        this.player = luraPlayer
        addEventHandler()
    }

    fun destroy() {
    }

    fun reset() {
        currentAd = null
        currentAdBreak = null
        removeIconView()
        progressView.progress = 0
        progressView.max = 0
        adCountInfo.text = resources.getString(R.string.luraAd)
        timeView.text = ""
        adSkipButton.text = ""
        adSkipButton.isEnabled = false
        adSkipButton.visibility = GONE
    }
}