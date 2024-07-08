package com.akta.luraplayercontrols.ui

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.contains
import androidx.mediarouter.app.MediaRouteButton
import com.akta.luraplayer.api.LuraPlayer
import com.akta.luraplayer.api.configs.LuraConfiguration
import com.akta.luraplayer.api.data.content.LuraVideoType
import com.akta.luraplayer.api.enums.LuraCastTargets
import com.akta.luraplayer.api.enums.LuraMimeType
import com.akta.luraplayer.api.event.LuraEventData
import com.akta.luraplayer.api.event.LuraEventType
import com.akta.luraplayer.api.models.LuraTrack
import com.akta.luraplayer.api.models.LuraTracks
import com.akta.luraplayercontrols.R
import com.akta.luraplayercontrols.extensions.toMs
import com.akta.luraplayercontrols.extensions.toSecondsString
import com.akta.luraplayercontrols.utils.LuraControlsSharedPreferencesUtil
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.Dispatchers

@SuppressLint("InflateParams")
class LuraControlBarView : ConstraintLayout, SeekBar.OnSeekBarChangeListener,
    LuraPopupCallbacks, LuraControlBar {
    private val hideDelayMs = 3000L

    internal var onPiPTap: (() -> Unit)? = null
    private var showAlways: Boolean = false
    private val hideRunnable = Runnable { hide() }

    internal var isStartSeeking = false
    private var isDelayedLive = false

    private var player: LuraPlayer? = null
    private val titleView: TextView
    private val timeView: LuraTimeView
    private val playPauseButton: LuraImageButton
    internal val muteUnmuteButton: LuraImageButton
    private val settingsButton: LuraImageButton
    private val subtitlesButton: LuraImageButton
    internal val fullscreenButton: LuraImageButton
    private val pipButton: LuraImageButton
    private val seekForwardView: LuraSeekView
    private val seekBackwardView: LuraSeekView
    internal val seekBar: LuraSeekBarWithMarks
    private var captionTextView: LuraCaptionTextView? = null

    private var onPlayPauseClickListener: OnClickListener? = null
    private var seekButtonsClickListener: OnClickListener? = null
    private var settingsButtonClickListener: OnClickListener? = null
    private var subtitlesButtonClickListener: OnClickListener? = null
    private var pipButtonClickListener: OnClickListener? = null

    private val imageViewContainer: FrameLayout
    private val imageView: ImageView
    private val density: Float = context.resources.displayMetrics.density
    private val imageViewContainerBottomMargin = (density * 8).toInt()
    private val imageViewContainerRadius = density * 4
    private val imageViewContainerPadding = density * 2

    private val sharedPreferencesUtil: LuraControlsSharedPreferencesUtil =
        LuraControlsSharedPreferencesUtil.getInstance(context)

    private val castButton: MediaRouteButton = MediaRouteButton(context).apply {
        id = View.generateViewId()
        setAlwaysVisible(true)
    }

    private val isReplay: Boolean
        get() = player?.isContentEnded ?: false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val adMarks: DoubleArray
        get() {
            val outputDuration = player?.duration
            if (outputDuration == null || outputDuration <= 0.0) return DoubleArray(0)

            return player?.getIntersititals?.mapNotNull {
                if (it.watched) null
                else when (it.offset) {
                    0.0 -> 0.0
                    outputDuration -> 100.0
                    else -> 100 * (it.offset / outputDuration)
                }
            }?.toDoubleArray() ?: DoubleArray(0)
        }

    private fun addEventHandler() {
        player?.addListener(Dispatchers.Main) { event ->
            when (event.type) {
                LuraEventType.CONFIGURED -> videoLoadStart()
                LuraEventType.LOADED_MEDIA_INFO -> seekBar.setDots(adMarks)
                LuraEventType.INTERSTITIALS -> seekBar.setDots(adMarks)
                LuraEventType.PLAYING -> {
                    if (player?.isCasting(LuraCastTargets.CHROMECAST) == true)
                        isDelayedLive = false

                    setIsPlaying(true)
                }

                LuraEventType.ENDED -> seekBar.setDots(adMarks)
                LuraEventType.PAUSED -> {
                    if (player?.isLive == true) {
                        isDelayedLive = true
                        setTime(LuraEventData.TimeUpdate())
                    }
                    setIsPlaying(false)
                }

                LuraEventType.CASTING_REQUESTED -> {
                    this.showAlways = true
                    pipButton.visibility = GONE
                    show()
                }

                LuraEventType.CASTING_ENDED -> {
                    this.showAlways = false
                    pipButton.visibility = VISIBLE
                    show()
                }

                LuraEventType.AD_BREAK_COMPLETED -> seekBar.setDots(adMarks)

                LuraEventType.TRACKS_UPDATED -> {
                    onTracksUpdatedEvent(player?.getTracks())
                }

                else -> {}
            }
            when (val data = event.data) {
                is LuraEventData.TimeUpdate -> if (!isStartSeeking) setTime(data)
                is LuraEventData.VideoMetadata -> {
                    data.metadata.title?.let {
                        if (it.isNotEmpty()) setTitle(it)
                    }
                }

                is LuraEventData.ShowCaption -> {
                    captionTextView?.text = data.text
                }

                else -> {}
            }
        }
    }

    override fun setVisibility(visibility: Int) {
        val v = if (player?.getTimeUpdateData()?.type?.isAd == true) GONE else visibility
        super.setVisibility(v)
    }

    private fun onTracksUpdatedEvent(tracks: LuraTracks?) {
        if (tracks == null) return
        luraWindow.updateAdapters(tracks, player?.playbackSpeed, player?.isLive)
        checkButtons()
        val isSelectedTrackOff =
            tracks.caption.firstOrNull { it.active }?.isSelectedTrackOff ?: true
        updateCCButton(isSelectedTrackOff)
    }

    init {
        View.inflate(context, R.layout.lura_control_bar_view, this)
        titleView = findViewById(R.id.titleView)
        timeView = findViewById(R.id.luraTimeView)
        playPauseButton = findViewById(R.id.playPauseView)
        muteUnmuteButton = findViewById(R.id.muteUnmuteView)
        seekForwardView = findViewById(R.id.seekForwardView)
        seekBackwardView = findViewById(R.id.seekBackwardView)
        seekBar = findViewById(R.id.seekbarView)
        subtitlesButton = findViewById(R.id.subtitleView)
        settingsButton = findViewById(R.id.settingsView)
        fullscreenButton = findViewById(R.id.fullscreenView)
        pipButton = findViewById(R.id.pipView)
        imageView = ImageView(context).apply {
            id = View.generateViewId()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    outline?.setRoundRect(
                        0,
                        0,
                        view!!.width,
                        view.height,
                        imageViewContainerRadius
                    )
                }
            }
            clipToOutline = true
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER
            layoutParams = params
        }
        imageViewContainer = FrameLayout(context).apply {
            id = generateViewId()
            background = ContextCompat.getDrawable(context, R.drawable.trickplay_background)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            addView(imageView)
        }
        initClickListeners()
        setOnClickListeners()
    }

    private val luraWindow: LuraPopupWindow =
        LuraPopupWindow(context, sharedPreferencesUtil, this, this).apply {
            setOnDismissListener {
                if (!isReplay) postDelayed(hideRunnable, hideDelayMs)
            }
        }

    private fun initClickListeners() {
        onPlayPauseClickListener = OnClickListener {
            resetHideRunnable()
            when (it.contentDescription) {
                resources.getString(R.string.pause) -> player?.pause()
                resources.getString(R.string.play) -> player?.play()
                resources.getString(R.string.replay) -> player?.play()
                else -> {}
            }
        }
        seekButtonsClickListener = OnClickListener {
            resetHideRunnable()
            if (it.id == R.id.seekForwardView) player?.seek(15 * 1000)
            else if (it.id == R.id.seekBackwardView) player?.seek(-15 * 1000)
        }
        settingsButtonClickListener = OnClickListener {
            showPopupWindow(LuraPopupType.Settings)
        }
        subtitlesButtonClickListener = OnClickListener {
            val texts = player?.getTracks()?.caption
            val audios = player?.getTracks()?.audio

            val textSize = texts?.size ?: 0
            val audioSize = audios?.size ?: 0
            if (textSize > 0 && audioSize > 1) {
                showPopupWindow(LuraPopupType.CaptionsAndAudios)
            } else if (textSize == 0 && audioSize > 1) {
                showPopupWindow(LuraPopupType.Audio)
            } else if (textSize > 0 && audioSize == 1) {
                if (texts != null && texts.size == 1) {
                    val text = texts.first()
                    onSubtitleTap(if (text.active) LuraTrack.Text() else text)
                } else showPopupWindow(LuraPopupType.Captions)
            }
        }
        pipButtonClickListener = OnClickListener {
            if (onPiPTap != null) {
                onPiPTap?.invoke()
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        resetHideRunnable()
        return super.onKeyUp(keyCode, event)
    }

    internal fun resetHideRunnable() {
        if (!isReplay) {
            removeCallbacks(hideRunnable)
            postDelayed(hideRunnable, hideDelayMs)
        }
    }

    private fun setOnClickListeners() {
        playPauseButton.setOnClickListener(onPlayPauseClickListener)
        seekForwardView.setOnClickListener(seekButtonsClickListener)
        seekBackwardView.setOnClickListener(seekButtonsClickListener)
        seekBar.setOnSeekBarChangeListener(this)
        subtitlesButton.setOnClickListener(subtitlesButtonClickListener)
        settingsButton.setOnClickListener(settingsButtonClickListener)
        pipButton.setOnClickListener(pipButtonClickListener)
        timeView.setOnClickListener {
            isDelayedLive = false

            if (player?.isCasting(LuraCastTargets.CHROMECAST) == true)
                player?.play()
            else
                player?.catchLive()
        }
    }

    fun hide() {
        removeCallbacks(hideRunnable)
        if (!showAlways) {
            alpha = 1f
            animate().alpha(0f).setDuration(200).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.INVISIBLE
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
        }
    }

    fun show() {
        alpha = 0f
        visibility = View.VISIBLE
        animate().alpha(1f).setDuration(200).setListener(null)
        if (!showAlways && !isReplay) {
            postDelayed(hideRunnable, hideDelayMs)
        }
        val selectedTrack = player?.getTracks()?.caption?.firstOrNull { it.active }
        updateCCButton(selectedTrack?.isSelectedTrackOff ?: true)
    }

    private fun showPopupWindow(type: LuraPopupType) {
        luraWindow.updateAdapters(player?.getTracks(), player?.playbackSpeed, player?.isLive)
        luraWindow.show(type)
        removeCallbacks(hideRunnable)
    }

    private fun videoLoadStart() {
        val config = player?.getConfig() ?: return
        initThumbnails(config)
        val type = player?.videoType ?: LuraVideoType.NONE
        val visibility = if (type.isLive) GONE
        else VISIBLE
        seekForwardView.visibility = GONE
        seekBackwardView.visibility = GONE
        seekBar.visibility = visibility
        seekBar.setDots(adMarks)
        setTitle(config.content?.title ?: "")
        setTitle(player?.getConfig()?.content?.title ?: "")
        setTime(LuraEventData.TimeUpdate())
        checkButtons()
    }

    private fun initThumbnails(config: LuraConfiguration) {
        val bifImages = config.content?.media?.filter { it.type == LuraMimeType.IMAGE_BIF.type }
        if (bifImages.isNullOrEmpty()) return
        val media = bifImages.firstOrNull() ?: return
        val ratio = media.width.div(media.height.toFloat())
        val maxDistance = width.coerceAtMost(height).div(4f)
        val width = (maxDistance * ratio).toInt()
        val height = maxDistance.toInt()
        imageView.layoutParams.height = height
        imageView.layoutParams.width = width
        imageViewContainer.layoutParams.height = height + imageViewContainerPadding.toInt()
        imageViewContainer.layoutParams.width = width + imageViewContainerPadding.toInt()
    }

    internal fun setTime(event: LuraEventData.TimeUpdate) {
        val isLive = player?.isLive ?: return
        timeView.setTime(event, isLive, isDelayedLive)
        if (!isLive) {
            seekBar.setPosition(event.copy(duration = event.duration))
            seekBar.setBufferedProgress(player?.getBufferedEnd()?.toMs() ?: 0L)
        }
    }

    internal fun setReplay() {
        show()
        playPauseButton.apply {
            setImageResource(R.drawable.replay)
            contentDescription = resources.getString(R.string.replay)
        }
    }

    internal fun setIsPlaying(isPlaying: Boolean) {
        playPauseButton.apply {
            contentDescription = if (isPlaying) {
                setImageResource(R.drawable.ic_pause)
                resources.getString(R.string.pause)
            } else {
                setImageResource(R.drawable.ic_play)
                resources.getString(R.string.play)
            }
        }
    }

    fun setTitle(title: String) {
        titleView.text = title
    }

    fun addPlayer(player: LuraPlayer?) {
        this.player = player
        addEventHandler()
        checkButtons()
    }

    private fun checkButtons() {
        val tracks = player?.getTracks()
        subtitlesButton.isEnabled = tracks?.caption?.isNotEmpty() ?: false
        pipButton.isEnabled = onPiPTap != null
        playPauseButton.isEnabled = player?.getConfig() != null
    }

    fun destroy() {
        captionTextView = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            val config = player?.getConfig()
            if (config != null)
                initThumbnails(config)
        }

        if (luraWindow.isShowing)
            luraWindow.updateSize()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            removeCallbacks(hideRunnable)
            val posStr = progress.toLong().div(1000).toSecondsString()
            val durStr = this.seekBar.max.toLong().div(1000).toSecondsString()
            timeView.setTime(resources.getString(R.string.lura_video_time, posStr, durStr))
            setImage(progress)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        startSeeking()
        if (setImage(seekBar?.progress ?: 0)) {
            if (findViewById<ImageView>(imageViewContainer.id) == null) {
                addView(imageViewContainer)
            }
            createMarginForImageViewContainer(seekBar)
        }
    }

    internal fun startSeeking() {
        isStartSeeking = true
    }

    private fun createMarginForImageViewContainer(seekBar: SeekBar?) {
        seekBar ?: return

        val halfContainerWidth = imageViewContainer.width.div(2.0)
        val percent = seekBar.progress.toDouble().div(seekBar.max.toDouble())
        val totalWidth = seekBar.width - seekBar.paddingStart - seekBar.paddingEnd
        var position = totalWidth.toDouble().times(percent)

        if (position < halfContainerWidth - seekBar.paddingStart) {
            position = seekBar.paddingStart.toDouble()
        } else if (position + halfContainerWidth > totalWidth) {
            position = (totalWidth - imageViewContainer.width + seekBar.paddingEnd).toDouble()
        } else {
            position -= halfContainerWidth - seekBar.paddingStart
        }

        position = position.coerceIn(
            seekBar.paddingStart.toDouble(),
            (totalWidth - imageViewContainer.width + seekBar.paddingEnd).toDouble()
        )

        val set = ConstraintSet()
        set.clone(this)
        set.connect(
            imageViewContainer.id,
            ConstraintSet.BOTTOM,
            this.seekBar.id,
            ConstraintSet.TOP,
            imageViewContainerBottomMargin
        )
        set.connect(
            imageViewContainer.id,
            ConstraintSet.START,
            this.seekBar.id,
            ConstraintSet.START
        )
        set.applyTo(this)

        (imageViewContainer.layoutParams as MarginLayoutParams).apply {
            marginStart = position.toInt()
        }
    }

    private fun setImage(progress: Int): Boolean {
        val longProgress: Long = progress.toLong()
        val bitmap = player?.requestTrickPlayImage(timeMs = longProgress) ?: return false
        imageView.setImageBitmap(bitmap)
        createMarginForImageViewContainer(seekBar)
        return true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        isStartSeeking = false
        if (seekBar != null && player?.getTimeUpdateData()?.type?.isAd != true) {
            if (seekBar.isEnabled) {
                player?.seek(seekBar.progress.toLong())
            }
        }
        postDelayed(hideRunnable, hideDelayMs)
        removeView(imageViewContainer)
        imageView.setImageBitmap(null)
    }

    override fun onSubtitleTap(track: LuraTrack.Text) {
        player?.useTrack(track)
        updateCCButton(track.isSelectedTrackOff)
    }

    private fun updateCCButton(isSelectedTrackOff: Boolean) {
        if (isSelectedTrackOff) {
            subtitlesButton.setImageResource(R.drawable.ic_subtitle)
        } else {
            subtitlesButton.setImageResource(R.drawable.ic_subtitle_on)
        }
    }


    override fun onVideoTap(track: LuraTrack.Video) {
        player?.useTrack(track)
    }

    override fun onAudioTap(track: LuraTrack.Audio) {
        player?.useTrack(track)
    }

    override fun onPlaybackRateTap(playbackSpeed: Float) {
        player?.playbackSpeed = playbackSpeed
    }

    override fun onBackClick(type: LuraPopupType?) {
        if (type != null) {
            showPopupWindow(type)
        } else {
            showPopupWindow(LuraPopupType.Settings)
        }
    }

    override fun onRemoveCallback() {
        removeCallbacks(hideRunnable)
    }

    internal fun setCaptionTextView(textView: LuraCaptionTextView) {
        captionTextView = textView
        updateCaptionTextView()
    }

    private fun updateCaptionTextView() {
        captionTextView?.apply {
            setCaptionFont(sharedPreferencesUtil.captionsFont)
            setCaptionFontSize(sharedPreferencesUtil.captionsFontSize)
            setCaptionTextColor(sharedPreferencesUtil.captionsTextColor)
            setCaptionTextOpacity(
                sharedPreferencesUtil.getOpacityFromPercent(
                    sharedPreferencesUtil.captionsTextOpacity
                )
            )
            setCaptionBackgroundColor(sharedPreferencesUtil.captionsBackgroundColor)
            setCaptionBackgroundOpacity(
                sharedPreferencesUtil.getOpacityFromPercent(
                    sharedPreferencesUtil.captionsBackgroundOpacity
                )
            )
            setCaptionHighlightColor(sharedPreferencesUtil.captionsHighlightColor)
            setCaptionHighlightOpacity(
                sharedPreferencesUtil.getOpacityFromPercent(
                    sharedPreferencesUtil.captionsHighlightOpacity
                )
            )
            setCaptionCapitalize(sharedPreferencesUtil.captionsCapitalize)
        }
    }

    override fun onCaptionUpdateCallback() {
        updateCaptionTextView()
    }

    internal fun addChromecastButton() {
        if (!findViewById<LinearLayout>(R.id.top_button_container).contains(castButton)) {
            CastButtonFactory.setUpMediaRouteButton(context, castButton)
            findViewById<LinearLayout>(R.id.top_button_container).addView(castButton, 0)
        }
    }

    internal fun removeChromecastButton() {
        if (findViewById<LinearLayout>(R.id.top_button_container).contains(castButton)) {
            findViewById<LinearLayout>(R.id.top_button_container).removeView(castButton)
        }
    }

    internal fun reset() {
        setTitle("")
        subtitlesButton.apply {
            isEnabled = false
            setImageResource(R.drawable.ic_subtitle)
        }
        playPauseButton.apply {
            setImageResource(R.drawable.play)
            contentDescription = resources.getString(R.string.play)
        }
        setTime(LuraEventData.TimeUpdate())
        removeView(imageViewContainer)
        imageView.setImageBitmap(null)
        isDelayedLive = false
        isStartSeeking = false
        timeView.setTime(LuraEventData.TimeUpdate(), isLive = false, isDelayedLive = false)
        seekBar.setPosition(LuraEventData.TimeUpdate())
        seekBar.setBufferedProgress(0)
        updateCaptionTextView()
    }
}
