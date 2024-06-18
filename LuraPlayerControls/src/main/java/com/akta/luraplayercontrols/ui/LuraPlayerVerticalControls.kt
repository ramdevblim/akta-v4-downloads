package com.akta.luraplayercontrols.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.akta.luraplayer.api.LuraPlayer
import com.akta.luraplayer.api.data.ad.LuraAd
import com.akta.luraplayer.api.data.content.LuraAnnotation
import com.akta.luraplayer.api.enums.LuraCastTargets
import com.akta.luraplayer.api.enums.LuraMimeType
import com.akta.luraplayer.api.enums.LuraScreenState
import com.akta.luraplayer.api.event.LuraEventData
import com.akta.luraplayer.api.event.LuraEventType
import com.akta.luraplayer.api.logger.LuraLog
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.extensions.toMs
import com.akta.luraplayercontrols.ui.listeners.DoubleClickListener
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CancellationException


class LuraPlayerVerticalControls(
    context: Context,
    attrs: AttributeSet? = null,
) : ConstraintLayout(context, attrs) {
    private var posterJob: Job? = null
    private var fullscreenCallback: ((isFullscreen: Boolean) -> Unit)? = null

    private var useControls: Boolean = true
    private var showAlways: Boolean = false
    private var contentTime: Int = 0

    fun reset() {
        luraSplashView.reset()
        visibility = if (useControls) View.VISIBLE else View.GONE
        setError(false)
        setControlBarClickListeners()
        controlBar.reset()
        controlBar.visibility = visibilityForControlBar
        adBar.reset()
        adBar.visibility = if (useControls) View.INVISIBLE else View.GONE
        changeProgressVisibility(View.GONE)
    }

    private var player: LuraPlayer? = null
    internal val controlBar: LuraVerticalControlBarView
    private val adBar: LuraAdControlView
    private val previewView: ImageView
    private val luraAnnotationGuideline: Guideline
    private val luraAnnotationButton: Button
    private val luraCaptionView: LuraCaptionTextView
    private val errorView: ImageView
    private var indicator: CircularProgressIndicator
    private var fullscreenButtonClickListener: OnClickListener? = null
    private var onMuteUnmuteClickListener: OnClickListener? = null
    private var luraForwardSeekClickListener: OnClickListener? = null
    private var luraBackwardSeekClickListener: OnClickListener? = null
    private var luraSplashView: LuraSplashView

    private var isMute: Boolean = false

    private var touchListener: OnTouchListener? = null

    private val luraBackwardSeek: LuraSeekButton
    private val luraForwardSeek: LuraSeekButton

    private val fullscreenButton: LuraImageButton
        get() = controlBar.fullscreenButton

    private var annotation: LuraAnnotation? = null

    private val fullscreenAdButton: LuraImageButton
        get() = adBar.fullscreenButton


    private val muteUnmuteButton: LuraImageButton
        get() = controlBar.muteUnmuteButton

    private val adMuteUnmuteButton: LuraImageButton
        get() = adBar.muteUnmuteButton
    private val visibilityForControlBar
        get() = if (useControls)
            if (showAlways) VISIBLE
            else INVISIBLE
        else GONE

    private val isReplay: Boolean
        get() = player?.isContentEnded ?: false

    private val adBarVisibility = if (useControls) INVISIBLE else GONE

    init {
        View.inflate(context, R.layout.lura_player_vertical_controls_view, this)
        controlBar = findViewById(R.id.luraControlBar)
        adBar = findViewById(R.id.luraAdBar)
        previewView = findViewById(R.id.luraPreviewView)
        luraAnnotationGuideline = findViewById(R.id.annotation_guideline)
        luraAnnotationButton = findViewById(R.id.luraAnnotation)
        luraCaptionView = findViewById(R.id.luraCaptionView)
        errorView = findViewById(R.id.luraErrorView)
        indicator = findViewById(R.id.luraLoadingIndicator)
        luraBackwardSeek = findViewById(R.id.luraBackwardSeek)
        luraForwardSeek = findViewById(R.id.luraForwardSeek)
        luraSplashView = findViewById(R.id.lura_splash_view)
        useControls(true)
        changeFullscreenState(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initPiPCallbacks()
        }
        initFullscreenCallback()
        initClickListeners()
        controlBar.setCaptionTextView(luraCaptionView)
    }

    private val seekForwardRunnable = Runnable {
        controlBar.onStopTrackingTouch(controlBar.seekBar)
        luraForwardSeek.reverseTransaction()
    }

    private val seekBackwardRunnable = Runnable {
        controlBar.onStopTrackingTouch(controlBar.seekBar)
        luraBackwardSeek.reverseTransaction()
    }

    private fun initClickListeners() {
        luraSplashView.playReplayButton.setOnClickListener {
            luraSplashView.visibility = GONE
            when (it.contentDescription) {
                resources.getString(R.string.play) -> player?.play()
                resources.getString(R.string.replay) -> player?.play()
                else -> {}
            }
        }

        fullscreenButtonClickListener = OnClickListener {
            val fullContentDescription = resources.getString(R.string.fullscreen_on)
            val isFullscreen = it.contentDescription == fullContentDescription
            fullscreenCallback?.invoke(isFullscreen)
            controlBar.resetHideRunnable()
        }
        fullscreenAdButton.setOnClickListener(fullscreenButtonClickListener)
        fullscreenButton.setOnClickListener(fullscreenButtonClickListener)
        onMuteUnmuteClickListener = OnClickListener {
            controlBar.resetHideRunnable()
            if (isMute) player?.setMuted(false)
            else player?.setMuted(true)
        }
        luraForwardSeekClickListener = object : DoubleClickListener() {
            override fun onClick(v: View) {
                if (player?.isLive == false && controlBar.seekBar.isEnabled) super.onClick(v)
                if (!controlBar.isStartSeeking) {
                    showOrHideControlBar()
                }
            }

            override fun onDoubleClick(v: View) {
                luraForwardSeek.startTransition()
                v.removeCallbacks(seekForwardRunnable)
                val progress = controlBar.seekBar.progress + 15000
                val max = controlBar.seekBar.max
                if (progress >= max) {
                    controlBar.seekBar.progress = max
                    controlBar.onProgressChanged(controlBar.seekBar, max, true)
                } else {
                    controlBar.seekBar.progress = progress
                    controlBar.onProgressChanged(controlBar.seekBar, progress, true)
                }
                if (!controlBar.isStartSeeking) {
                    controlBar.startSeeking()
                }
                v.postDelayed(seekForwardRunnable, 300)
            }
        }
        luraBackwardSeekClickListener = object : DoubleClickListener() {
            override fun onClick(v: View) {
                if (player?.isLive == false && controlBar.seekBar.isEnabled) super.onClick(v)
                if (!controlBar.isStartSeeking) {
                    showOrHideControlBar()
                }
            }

            override fun onDoubleClick(v: View) {
                luraBackwardSeek.startTransition()
                v.removeCallbacks(seekBackwardRunnable)
                val progress = controlBar.seekBar.progress - 15000
                if (progress <= 0) {
                    controlBar.seekBar.progress = 0
                    controlBar.onProgressChanged(controlBar.seekBar, 0, true)
                } else {
                    controlBar.seekBar.progress = progress
                    controlBar.onProgressChanged(controlBar.seekBar, progress, true)
                }
                controlBar.startSeeking()
                v.postDelayed(seekBackwardRunnable, 300)
            }
        }
        muteUnmuteButton.setOnClickListener(onMuteUnmuteClickListener)
        adMuteUnmuteButton.setOnClickListener(onMuteUnmuteClickListener)
        luraAnnotationButton.setOnClickListener {
            val annotation = this.annotation ?: return@setOnClickListener
            player?.seek(annotation.end.toMs())
        }
        setControlBarClickListeners()
    }

    private fun setControlBarClickListeners() {
        luraForwardSeek.setOnClickListener(luraForwardSeekClickListener)
        luraBackwardSeek.setOnClickListener(luraBackwardSeekClickListener)
        controlBar.seekBar.isEnabled = true
        this.setOnClickListener { showOrHideControlBar() }
    }

    private fun removeControlBarClickListeners() {
        controlBar.seekBar.isEnabled = false
        val timeUpdate = player?.getTimeUpdateData()
        if (timeUpdate != null) controlBar.setTime(timeUpdate)
        controlBar.onStopTrackingTouch(controlBar.seekBar)
    }

    private fun changeFullscreenState(isFullscreenOn: Boolean) {
        val isFullscreen = fullscreenButton.contentDescription
            .equals(resources.getString(R.string.fullscreen_on))
        if (isFullscreen == isFullscreenOn) return
        val (drawable, description) = if (isFullscreenOn) {
            Pair(R.drawable.fullscreen_off, resources.getString(R.string.fullscreen_on))
        } else {
            Pair(R.drawable.ic_fullscreen, resources.getString(R.string.fullscreen))
        }
        fullscreenAdButton.apply {
            setImageResource(drawable)
            contentDescription = description
        }
        fullscreenButton.apply {
            setImageResource(drawable)
            contentDescription = description
        }
    }

    private fun setMuteState(event: LuraEventData.MuteChanged) {
        this.isMute = event.muted
        if (isMute) {
            muteUnmuteButton.setImageResource(R.drawable.ic_mute_on)
            adMuteUnmuteButton.setImageResource(R.drawable.ic_mute_on)
        } else {
            muteUnmuteButton.setImageResource(R.drawable.ic_mute_off)
            adMuteUnmuteButton.setImageResource(R.drawable.ic_mute_off)
        }
    }

    private fun addEventHandler() {
        player?.addListener(Dispatchers.Main) { event ->
            when (event.type) {
                LuraEventType.CONFIGURED -> {
                    loadPoster()
                    val config = player?.getConfig()
                    setMuteState(
                        LuraEventData.MuteChanged(
                            muted = player?.isMuted ?: false
                        )
                    )
                    val castEnabled = config?.cast?.chromecast?.enabled ?: false
                    if (castEnabled) {
                        addChromecastButton()
                    } else {
                        removeChromecastButton()
                    }
                    if (player?.isCasting(LuraCastTargets.CHROMECAST) == false)
                        setSplash()

                    useControls(config?.controls?.enabled ?: true)
                }

                LuraEventType.PLAYING -> {
                    if (adBar.currentAd?.interactiveFiles?.firstOrNull { it.isSimid } != null) {
                        this.visibility = View.GONE
                    }
                    luraSplashView.visibility = GONE
                    setIsPlaying(true)
                    if (!showAlways && !isReplay) previewView.visibility = GONE
                    changeProgressVisibility(View.GONE)
                }

                LuraEventType.REPLAY -> {
                    setError(false)
                    adBar.visibility = adBarVisibility
                }

                LuraEventType.PAUSED -> {
                    setIsPlaying(false)
                }

                LuraEventType.SCREEN_STATE_CHANGED -> {
                    val data = event.data as LuraEventData.ScreenState
                    when (data.state) {
                        LuraScreenState.FULLSCREEN -> changeFullscreenState(true)
                        LuraScreenState.PICTURE_IN_PICTURE -> {}
                        LuraScreenState.WINDOWED -> changeFullscreenState(false)
                    }
                }

                LuraEventType.ENDED -> {
                    adBar.visibility = GONE
                    luraAnnotationButton.visibility = View.GONE
                    changeProgressVisibility(View.GONE)
                    setReplay()
                }

                LuraEventType.CASTING_REQUESTED -> {
                    adBar.reset()
                    adBar.visibility = adBarVisibility
                    this.showAlways = true
                    previewView.visibility = VISIBLE
                }

                LuraEventType.CASTING_ENDED -> {
                    adBar.reset()
                    adBar.visibility = adBarVisibility
                    setControlBarClickListeners()
                    this.showAlways = false
                    previewView.visibility = GONE
                }

                LuraEventType.AD_BREAK_STARTED -> {
                    removeControlBarClickListeners()
                    controlBar.visibility = View.GONE
                    luraCaptionView.visibility = View.GONE
                }

                LuraEventType.AD_STARTED -> {
                    adBar.setSkipPreview(contentTime)
                    controlBar.visibility = View.GONE
                    luraCaptionView.visibility = View.GONE
                    val ad = (event.data as? LuraAd)
                    val adBreak = adBar.currentAdBreak
                    adBar.setAdStart(ad)
                    val visibility = if (useControls && adBreak?.stitcher != "GAM") VISIBLE
                    else GONE
                    if (ad?.interactiveFiles?.firstOrNull {
                            it.apiFramework?.equals("simid", true) != false
                        } == null) {
                        adBar.visibility = visibility
                        this.visibility = visibility
                    }
                    if (ad?.interactiveFiles?.firstOrNull { it.isSimid } != null) {
                        if (player?.isPlaying == true) {
                            this.visibility = View.GONE
                        }
                    }
                    if (adBreak?.stitcher == "GAM") this.visibility = GONE
                }

                LuraEventType.AD_COMPLETED -> {
                    adBar.visibility = GONE
                }

                LuraEventType.AD_SKIPPED -> {
                    adBar.visibility = GONE
                }

                LuraEventType.AD_BREAK_COMPLETED,
                -> {
                    controlBar.visibility = visibilityForControlBar
                    luraCaptionView.visibility = View.VISIBLE
                    adBar.visibility = GONE
                    visibility = VISIBLE
                    setControlBarClickListeners()
                }

                LuraEventType.TIME_UPDATED -> {
                    contentTime = (event.data as LuraEventData.TimeUpdate).contentTimeMs.toInt()
                    updateAnnotationButton(event.data as LuraEventData)
                }

                LuraEventType.BUFFERING_STARTED -> changeProgressVisibility(VISIBLE)
                LuraEventType.BUFFERING_ENDED -> {
                    setIsPlaying(player?.isPlaying ?: false)
                    changeProgressVisibility(GONE)
                }

                LuraEventType.SEEKED -> setError(false)
                LuraEventType.ERROR -> {
                    setReplay()
                    setError(true)
                    adBar.visibility = GONE
                    changeProgressVisibility(GONE)
                }

                else -> {}
            }
            when (val data = event.data) {
                is LuraEventData.MuteChanged -> setMuteState(data)
                else -> {}
            }
        }
    }

    private fun updateAnnotationButton(data: LuraEventData?) {
        val ad = (data as? LuraEventData.TimeUpdate)?.ad
        annotation = (data as? LuraEventData.TimeUpdate)?.annotations?.firstOrNull()
        luraAnnotationButton.apply {
            visibility = if (
                annotation != null && ad == null && useControls
            ) {
                if (!isFocused) requestFocus()
                VISIBLE
            } else GONE
            val annotationString = if (annotation?.type?.equals("intro", false) == true) {
                context.getString(R.string.luraAnnotationIntro)
            } else if (annotation?.type?.equals("outro", false) == true) {
                context.getString(R.string.luraAnnotationOutro)
            } else {
                null
            }
            text = annotation?.label ?: annotationString ?: annotation?.type
        }
    }

    private fun setError(isError: Boolean) {
        errorView.visibility = if (useControls) {
            if (isError) VISIBLE else GONE
        } else GONE
        controlBar.seekBar.isEnabled = !isError
    }

    fun useControls(enabled: Boolean) {
        this.useControls = enabled
        controlBar.visibility = visibilityForControlBar
        adBar.visibility = if (enabled) INVISIBLE else GONE
        luraForwardSeek.visibility = if (enabled) VISIBLE else GONE
        luraBackwardSeek.visibility = if (enabled) VISIBLE else GONE
    }

    private fun initFullscreenCallback() {
        this.fullscreenCallback = fun(isFullscreen: Boolean) {
            if (isFullscreen) {
                player?.setScreenState(LuraScreenState.WINDOWED)
            } else {
                player?.setScreenState(LuraScreenState.FULLSCREEN)
            }
        }
        fullscreenAdButton.isEnabled = fullscreenCallback != null
        fullscreenButton.isEnabled = fullscreenCallback != null
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun initPiPCallbacks() {
        val callback = fun() {
            player?.setScreenState(LuraScreenState.PICTURE_IN_PICTURE)
        }
        controlBar.onPiPTap = callback
        adBar.onPiPTap = callback
    }

    private fun loadPoster() {
        if (posterJob?.isActive == true) {
            posterJob?.cancel(CancellationException())
        }
        val config = player?.getConfig()
        val assetID = config?.lura?.assetId ?: "NA"
        val fileName = "poster_$assetID.jpg"
        val file = File(context.cacheDir, fileName)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.path)
            previewView.setImageBitmap(bitmap)
            adBar.setSkipPreview(bitmap)
            luraSplashView.setPoster(bitmap)
        } else {
            val posterUrl = config?.content?.media
                ?.filter { it.type != LuraMimeType.IMAGE_BIF.type }
                ?.firstOrNull { it.type.contains("image") }?.url
            if (posterUrl.isNullOrEmpty()) return
            posterJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val input: InputStream = withContext(Dispatchers.IO) {
                        URL(posterUrl).openStream()
                    }
                    val bitmap: Bitmap = BitmapFactory.decodeStream(input)
                    posterJob = CoroutineScope(Dispatchers.Main).launch {
                        previewView.setImageBitmap(bitmap)
                        adBar.setSkipPreview(bitmap)
                        luraSplashView.setPoster(bitmap)
                    }
                } catch (e: Exception) {
                    LuraLog.e("Error", e.message ?: "")
                }
            }
        }
    }

    fun changeProgressVisibility(visibility: Int) {
        indicator.visibility = if (useControls) visibility else View.GONE
    }

    private fun showOrHideControlBar() {
        if ((showAlways || isReplay) && useControls) controlBar.show()
        else
            when (controlBar.visibility) {
                VISIBLE -> controlBar.hide()
                INVISIBLE -> controlBar.show()
                else -> {}
            }
    }

    private fun setReplay() {
        controlBar.setReplay()
        adBar.setReplay()
        previewView.visibility = VISIBLE
    }

    private fun setSplash() {
        luraSplashView.setSplash()
    }

    private fun setIsPlaying(isPlaying: Boolean) {
        controlBar.setIsPlaying(isPlaying)
        adBar.setIsPlaying(isPlaying)
    }

    internal fun destroy() {
        controlBar.destroy()
        adBar.destroy()
    }

    fun addPlayer(luraPlayer: LuraPlayer?) {
        this.player = luraPlayer
        controlBar.addPlayer(luraPlayer)
        adBar.addPlayer(luraPlayer)
        addEventHandler()
        setMuteState(LuraEventData.MuteChanged(muted = luraPlayer?.isMuted ?: true))
    }

    private fun addChromecastButton() {
        controlBar.addChromecastButton()
        adBar.addChromecastButton()
    }

    private fun removeChromecastButton() {
        controlBar.removeChromecastButton()
        adBar.removeChromecastButton()
    }

    fun updateConfig() {
        controlBar.setTitle(player?.getConfig()?.content?.title ?: "")
    }

    internal fun hideAdBar() {
        adBar.visibility = View.GONE
    }

    fun setTouchListener(listener: OnTouchListener) {
        touchListener = listener
    }

    fun clearTouchListener() {
        touchListener = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        touchListener?.onTouch(this, event)
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        touchListener?.onTouch(this, ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val newOrientation: Int = newConfig.orientation

        (luraAnnotationGuideline.layoutParams as LayoutParams).apply {
            guidePercent = when (newOrientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    0.75F
                }

                Configuration.ORIENTATION_PORTRAIT -> {
                    0.65F
                }

                else -> 0.70F
            }
            luraAnnotationGuideline.layoutParams = this
        }
    }
}