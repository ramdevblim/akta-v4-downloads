package com.akta.luraplayersampleapp.modern.screens

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.akta.luraplayer.api.LuraEventListener
import com.akta.luraplayer.api.LuraPlayer
import com.akta.luraplayer.api.data.ad.LuraAd
import com.akta.luraplayer.api.data.ad.LuraAdBreak
import com.akta.luraplayer.api.enums.LuraExceptionType
import com.akta.luraplayer.api.enums.LuraScreenState
import com.akta.luraplayer.api.event.LuraEvent
import com.akta.luraplayer.api.event.LuraEventData
import com.akta.luraplayer.api.event.LuraEventType
import com.akta.luraplayer.api.logger.LuraLog
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayer.api.ui.LuraPlayerView
import com.akta.luraplayercontrols.ui.LuraPlayerControls
import com.akta.luraplayercontrols.ui.LuraPlayerVerticalControls
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.adapters.ModernVideoListAdapter
import com.akta.luraplayersampleapp.modern.data.Video
import com.akta.luraplayersampleapp.modern.data.toLura
import com.akta.luraplayersampleapp.modern.dialogs.LuraAlertDialog
import com.akta.luraplayersampleapp.modern.events.ScreenStateEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.EventBus

class ModernPlayerFragment : Fragment(R.layout.modern_fragment_player) {

    companion object {
        const val TAG = "ModernPlayerFragment"
    }

    private enum class PlayerLayout {
        DEFAULT,
        VERTICAL
    }

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var luraPlayerView: LuraPlayerView
    private lateinit var luraPlayerControls: LuraPlayerControls
    private lateinit var luraPlayerVerticalControls: LuraPlayerVerticalControls
    private lateinit var dragLayout: FrameLayout
    private lateinit var headerLayout: TextView
    private lateinit var videoListRecyclerView: RecyclerView

    private var luraPlayer: LuraPlayer? = null
    private var luraOffline: LuraOfflineManager? = null
    private var luraPlayerScreenState: LuraScreenState = LuraScreenState.WINDOWED
    private var playerLayout: PlayerLayout = PlayerLayout.DEFAULT
    private var isInPictureInPicture: Boolean = false
    private var maxAllowedHeight: Double = 0.0
    private var initialDragViewY = 0F
    private var isPlayerViewExpanded = false
    private var playerViewMinRatio = 0f
    private var playerViewMaxRatio = 16f / 9f
    private var initialRatio = 0f
    private var transitionDuration: Int = 400

    private val listener: LuraEventListener = LuraEventListener { event ->
        if (!isAdded) return@LuraEventListener

        if (event.type != LuraEventType.BUFFER_CHANGED && event.type != LuraEventType.TIME_UPDATED) {
            LuraLog.d(TAG, event.type.name)
        }

        when (val data = event.data) {
            is LuraEventData.Error -> {
                val (title, message) = when (data.exception.type) {
                    LuraExceptionType.LicenseExpired -> {
                        Pair(resources.getString(R.string.warning), data.exception.message)
                    }

                    else -> {
                        Pair(
                            resources.getString(R.string.warning),
                            "Please check your internet connection and try again"
                        )
                    }
                }

                LuraAlertDialog(
                    requireContext(),
                    title,
                    message
                ).show()

                LuraLog.d(
                    TAG,
                    "Error: ${data.exception.cause?.stackTraceToString()}"
                )
            }

            is LuraAd,
            is LuraAdBreak
            -> handledAdEvents(event)

            is LuraEventData.VideoMetadata -> {
                LuraLog.d(
                    TAG,
                    "VideoMetadata: ${event.type} Data: ${event.data}"
                )
            }

            is LuraEventData.TimeUpdate -> {}
            is LuraEventData.ShowCaption -> LuraLog.d(
                TAG,
                "Caption: ${data.text}"
            )

            is LuraEventData.RateChanged -> {}
            is LuraEventData.VolumeChanged -> {}

            is LuraEventData.TrackChanged -> {}

            is LuraEventData.ScreenState -> {
                luraPlayerScreenState = data.state
                setPlayerViewConstraints(luraPlayerScreenState)
            }

            is LuraEventData.Warning -> {
                LuraLog.d(TAG, "Warning: ${event.type}, Data: ${event.data}")
            }

            null -> {
                handleDefaultEvents(event)
            }

            else -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transitionDuration = resources.getInteger(R.integer.animation_duration)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_from_bottom)
        exitTransition = inflater.inflateTransition(R.transition.slide_from_bottom)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootLayout = view.findViewById(R.id.root)
        luraPlayerView = view.findViewById(R.id.lura_player_view)
        luraPlayerControls = view.findViewById(R.id.lura_player_controls)
        luraPlayerVerticalControls = view.findViewById(R.id.lura_player_vertical_controls)
        videoListRecyclerView = view.findViewById(R.id.video_list)
        dragLayout = view.findViewById(R.id.drag_layout)
        dragLayout.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    playerViewMinRatio = rootLayout.width.toFloat() / (rootLayout.height / 3f * 2f)
                    initialRatio = luraPlayerView.width.toFloat() / luraPlayerView.height
                    initialDragViewY = event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialDragViewY
                    var newRatio =
                        initialRatio - deltaY * 0.0015f // Adjust this value to control sensitivity
                    newRatio = newRatio.coerceIn(playerViewMinRatio, playerViewMaxRatio)
                    ConstraintSet().apply {
                        clone(rootLayout)
                        setDimensionRatio(R.id.lura_player_view, newRatio.toString())
                        applyTo(rootLayout)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val currentRatio = luraPlayerView.width.toFloat() / luraPlayerView.height
                    val midPoint = playerViewMaxRatio - playerViewMinRatio
                    val desiredRatio = if (currentRatio > midPoint)
                        playerViewMaxRatio
                    else
                        playerViewMinRatio
                    ConstraintSet().apply {
                        clone(rootLayout)
                        setDimensionRatio(R.id.lura_player_view, desiredRatio.toString())
                        TransitionManager.beginDelayedTransition(rootLayout)
                        applyTo(rootLayout)
                    }
                }
            }
            return@setOnTouchListener true
        }
        headerLayout = view.findViewById(R.id.header)
        headerLayout.setOnClickListener {
            // Dummy
        }

        luraOffline = LuraOfflineManager(context = requireContext())
        luraPlayer = LuraPlayer(context = requireContext(), playerView = luraPlayerView).apply {
            addListener(dispatcher = Dispatchers.Main, listener = listener)
        }
        luraPlayerControls.addPlayer(luraPlayer = luraPlayer)
        luraPlayerVerticalControls.addPlayer(luraPlayer = luraPlayer)
        luraPlayer?.friendlyViews = listOf(luraPlayerControls, luraPlayerVerticalControls)

        videoListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        videoListRecyclerView.adapter = ModernVideoListAdapter(
            activity = requireActivity(),
            luraOffline = luraOffline!!,
            onStopClick = {
                luraPlayer?.stop()
            }
        ) { item -> playVideo(item) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            luraPlayerView.doOnLayout {
                updatePictureInPictureParams()
            }
        }

        rootLayout.doOnLayout {
            maxAllowedHeight = rootLayout.width.toDouble() / (rootLayout.height / 3.0 * 2)
        }

        arguments?.let {
            val videosJsonStr = it.getString("videos") ?: "[]"
            val selectedPosition = it.getInt("selectedPosition", 0)

            val videos = Json.decodeFromString<List<Video>>(videosJsonStr)

            videoListRecyclerView.post {
                (videoListRecyclerView.adapter as ModernVideoListAdapter).updateVideos(
                    videos,
                    selectedPosition
                )
                videoListRecyclerView.scrollToPosition(selectedPosition)
            }

            playVideo(videos[selectedPosition])
        }
    }

    private fun handleDefaultEvents(event: LuraEvent) {
        when (event.type) {
            LuraEventType.LOADED_MEDIA_INFO -> {
                if (luraPlayerScreenState == LuraScreenState.WINDOWED)
                    setPlayerViewConstraints(LuraScreenState.WINDOWED)
            }

            LuraEventType.END_OF_PARTIAL_CONTENT -> {
                LuraAlertDialog(
                    requireContext(),
                    resources.getString(R.string.warning),
                    resources.getString(R.string.end_of_partial_content)
                ).show()
            }

            LuraEventType.PLAYING -> {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            LuraEventType.ENDED,
            LuraEventType.ERROR,
            LuraEventType.PAUSED,
            -> {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            else -> {}
        }
    }

    private fun handledAdEvents(event: LuraEvent) {
        when (event.type) {
            LuraEventType.AD_BREAK_STARTED -> {}
            LuraEventType.AD_STARTED -> {}
            LuraEventType.AD_FIRST_QUARTILE -> {}
            LuraEventType.AD_MIDPOINT -> {}
            LuraEventType.AD_THIRD_QUARTILE -> {}
            LuraEventType.AD_COMPLETED -> {}
            LuraEventType.AD_BREAK_COMPLETED -> {}
            LuraEventType.AD_SKIPPED -> {}
            LuraEventType.AD_TAPPED -> {}
            LuraEventType.AD_CLICKED -> {}
            else -> {}
        }
    }

    private fun playVideo(video: Video) {
        try {
            val config = video.toLura()
            luraPlayer?.setConfig(config)
            setLayout(config.controls.layout)
            if (playerLayout == PlayerLayout.VERTICAL) {
                luraPlayerVerticalControls.reset()
                luraPlayerVerticalControls.changeProgressVisibility(View.VISIBLE)
            } else {
                luraPlayerControls.reset()
                luraPlayerControls.changeProgressVisibility(View.VISIBLE)
            }
        } catch (e: Exception) {
            LuraLog.e(TAG, e.toString())
        }
    }

    private fun setLayout(layout: String) {
        when (layout) {
            "default" -> {
                playerLayout = PlayerLayout.DEFAULT
                dragLayout.visibility = View.GONE
                rootLayout.removeView(luraPlayerVerticalControls)
                if (luraPlayerControls.parent == null)
                    rootLayout.addView(luraPlayerControls)
            }

            "vertical" -> {
                playerLayout = PlayerLayout.VERTICAL
                dragLayout.visibility = View.VISIBLE
                rootLayout.removeView(luraPlayerControls)
                if (luraPlayerVerticalControls.parent == null)
                    rootLayout.addView(luraPlayerVerticalControls)
            }
        }

        setPlayerViewConstraints(LuraScreenState.WINDOWED)
    }

    private fun setPlayerViewConstraints(state: LuraScreenState) {
        if (state == LuraScreenState.PICTURE_IN_PICTURE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPicture()
            }
            return
        }

        val isVertical = playerLayout == PlayerLayout.VERTICAL

        EventBus.getDefault().post(ScreenStateEvent(state, isVertical))

        rootLayout.postDelayed({
            ConstraintSet().apply {
                clone(rootLayout)
                clear(R.id.lura_player_view, ConstraintSet.BOTTOM)

                if (state == LuraScreenState.WINDOWED) {
                    val ratio = if (isVertical) {
                        isPlayerViewExpanded = true
                        "$maxAllowedHeight:1"
                    } else {
                        isPlayerViewExpanded = false
                        val video = luraPlayer?.getTracks()?.video?.firstOrNull()
                        val (width, height) = Pair(
                            video?.width ?: 16,
                            video?.height ?: 9
                        )
                        if (width.toDouble() / height < 16.0 / 9.0) {
                            "$width:$height"
                        } else {
                            "16:9"
                        }
                    }
                    setDimensionRatio(R.id.lura_player_view, ratio)
                } else if (state == LuraScreenState.FULLSCREEN) {
                    setDimensionRatio(R.id.lura_player_view, null)
                    connect(
                        R.id.lura_player_view,
                        ConstraintSet.BOTTOM,
                        R.id.root,
                        ConstraintSet.BOTTOM
                    )
                }

                TransitionManager.beginDelayedTransition(rootLayout)
                applyTo(rootLayout)
            }
        }, 125)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureParams(): PictureInPictureParams {
        val visibleRect = Rect()
        luraPlayerView.getGlobalVisibleRect(visibleRect)
        var rational = Rational(luraPlayerView.width, luraPlayerView.height)

        // Maximum allowed aspect ratio is 2.39, (239 / 100 = 2.39)
        if (rational.numerator.toFloat() / rational.denominator > 2.39) {
            rational = Rational(16, 9)
        }

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .setSourceRectHint(visibleRect)
            .build()
        requireActivity().setPictureInPictureParams(params)

        return params
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPictureInPicture() {
        isInPictureInPicture =
            requireActivity().enterPictureInPictureMode(updatePictureInPictureParams())
    }

    internal fun setScreenState(state: LuraScreenState) {
        luraPlayer?.setScreenState(state)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        isInPictureInPicture = isInPictureInPictureMode
        if (playerLayout == PlayerLayout.VERTICAL)
            luraPlayerVerticalControls.useControls(!isInPictureInPicture)
        else
            luraPlayerControls.useControls(!isInPictureInPicture)
    }

    override fun onResume() {
        super.onResume()
        luraPlayer?.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPicture)
            luraPlayer?.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (isInPictureInPicture)
            luraPlayer?.onPause()
    }

    override fun onDestroyView() {
        luraPlayerControls.clearTouchListener()
        luraPlayerVerticalControls.clearTouchListener()
        super.onDestroyView()
    }

    override fun onDestroy() {
        (videoListRecyclerView.adapter as ModernVideoListAdapter).destroy()
        luraOffline = null
        luraPlayer?.removeListener(listener = listener)
        luraPlayer?.destroy()
        luraPlayer = null
        super.onDestroy()
    }
}