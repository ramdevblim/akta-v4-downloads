package com.akta.luraplayersampleapp.modern.screens

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.OrientationEventListener
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import androidx.mediarouter.app.MediaRouteButton
import com.akta.luraplayer.api.cast.LuraOptionsProvider
import com.akta.luraplayer.api.constants.LuraPlayerLogLevel
import com.akta.luraplayer.api.enums.LuraDownloadRequirement
import com.akta.luraplayer.api.enums.LuraScreenState
import com.akta.luraplayer.api.logger.LuraLog
import com.akta.luraplayer.api.offline.LuraOfflineManager
import com.akta.luraplayersampleapp.R
import com.akta.luraplayersampleapp.modern.events.AssetSelectedEvent
import com.akta.luraplayersampleapp.modern.events.EditButtonPressedEvent
import com.akta.luraplayersampleapp.modern.events.ScreenStateEvent
import com.akta.luraplayersampleapp.modern.events.ScrollToPositionEvent
import com.akta.luraplayersampleapp.modern.screens.downloads.DownloadsFragment
import com.akta.luraplayersampleapp.modern.screens.home.HomeFragment
import com.akta.luraplayersampleapp.modern.screens.settings.SettingsFragment
import com.akta.luraplayersampleapp.modern.utils.SharedPreferencesUtil
import com.akta.luraplayersampleapp.modern.DemoAppDownloadService
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.Executors

class ModernMainActivity : AppCompatActivity() {

    enum class NavigationScreen {
        HOME,
        DOWNLOADS,
        SETTINGS
    }

    companion object {
        var currentOrientation: Int = Configuration.ORIENTATION_UNDEFINED
    }

    private lateinit var motionRoot: MotionLayout
    private lateinit var bottomNavigationView: BottomNavigationView

    private var luraOffline: LuraOfflineManager? = null
    private var playerFragment: ModernPlayerFragment? = null
    private var orientationEventListener: OrientationEventListener? = null
    private var editButton: MenuItem? = null
    private var isFullscreen: Boolean = false
    private var isInPictureInPicture: Boolean = false

    private val prefUtil: SharedPreferencesUtil
        get() = SharedPreferencesUtil.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.modern_activity_main)

        CastContext.getSharedInstance(this, Executors.newSingleThreadExecutor())
        val appBar = findViewById<AppBarLayout>(R.id.action_bar)
        val toolbar = appBar.findViewById<Toolbar>(R.id.custom_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getString(R.string.app_name)
        val mediaRouteButton = toolbar.findViewById<MediaRouteButton>(R.id.media_route_button)
        CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton)

        currentOrientation = resources.configuration.orientation

        LuraLog.setLogLevel(logLevel = LuraPlayerLogLevel.Errors)
        LuraOfflineManager.clazz = DemoAppDownloadService::class.java
        luraOffline = LuraOfflineManager(context = this)

        val wifiOnly = prefUtil.wifiOnly
        val offlineRequirement =
            if (wifiOnly) LuraDownloadRequirement.WIFI else LuraDownloadRequirement.ANY
        luraOffline?.setRequirements(offlineRequirement)
        luraOffline?.maxParallelDownloads = prefUtil.maxParallelDownloads

        motionRoot = findViewById(R.id.root)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    displayMainFragment(NavigationScreen.HOME)
                }

                R.id.nav_downloads -> {
                    displayMainFragment(NavigationScreen.DOWNLOADS)
                }

                R.id.nav_settings -> {
                    displayMainFragment(NavigationScreen.SETTINGS)
                }
            }
            return@setOnItemSelectedListener true
        }

        bottomNavigationView.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.nav_settings -> {
                    val count = supportFragmentManager.backStackEntryCount
                    if (count > 0) {
                        @Suppress("DEPRECATION")
                        onBackPressed()
                    }
                }
            }
        }

        orientationEventListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val topPortrait = 0
                val bottomPortrait = 360
                val leftLandscape = 90
                val rightLandscape = 270

                if (isFullscreen) {
                    if (epsilonCheck(orientation, leftLandscape) ||
                        epsilonCheck(orientation, rightLandscape)
                    ) {
                        requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                        orientationEventListener?.disable()
                    }
                } else {
                    if (epsilonCheck(orientation, topPortrait) ||
                        epsilonCheck(orientation, bottomPortrait)
                    ) {
                        requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                        orientationEventListener?.disable()
                    }
                }
            }

            private fun epsilonCheck(a: Int, b: Int): Boolean {
                val epsilon = 10
                return a > b - epsilon && a < b + epsilon
            }
        }

        addOnPictureInPictureModeChangedListener {
            isInPictureInPicture = it.isInPictureInPictureMode
        }

        displayMainFragment(NavigationScreen.HOME)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun displayMainFragment(screen: NavigationScreen) {
        editButton?.isVisible = false

        when (screen) {
            NavigationScreen.HOME -> {
                val homeFragment = HomeFragment()
                supportFragmentManager.commit {
                    replace(R.id.main_fragment, homeFragment, HomeFragment.TAG)
                }
                motionRoot.transitionToState(R.id.set_main)
                motionRoot.transitionToStart()
            }

            NavigationScreen.DOWNLOADS -> {
                val downloadsFragment = DownloadsFragment()
                supportFragmentManager.commit {
                    replace(R.id.main_fragment, downloadsFragment, DownloadsFragment.TAG)
                }
                editButton?.isVisible = true
                motionRoot.transitionToState(R.id.set_main)
                motionRoot.transitionToStart()
            }

            NavigationScreen.SETTINGS -> {
                val settingsFragment = SettingsFragment()
                supportFragmentManager.commit {
                    replace(R.id.main_fragment, settingsFragment, SettingsFragment.TAG)
                }
                motionRoot.transitionToState(R.id.set_main)
                motionRoot.transitionToStart()
            }
        }
    }

    private fun destroyPlayerFragment() {
        motionRoot.transitionToState(R.id.set_player)
        motionRoot.transitionToStart()
        supportFragmentManager.findFragmentByTag(ModernPlayerFragment.TAG)?.let {
            supportFragmentManager.commit {
                remove(it)
            }
        }
        playerFragment = null
    }

    private fun setActionAndSystemBars(show: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (show) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAssetSelectedEvent(event: AssetSelectedEvent) {
        val selectedPosition = event.selectedAssetPosition
        val assets = event.assets

        val arguments = Bundle()
        arguments.putString("videos", assets)
        arguments.putInt("selectedPosition", selectedPosition)

        if (playerFragment == null) {
            playerFragment = ModernPlayerFragment()
            playerFragment!!.arguments = arguments

            supportFragmentManager.commit {
                add(R.id.player_fragment, playerFragment!!, ModernPlayerFragment.TAG)
            }

            motionRoot.transitionToState(R.id.set_player)
            motionRoot.transitionToEnd()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onScreenStateEvent(event: ScreenStateEvent) {
        when (event.state) {
            LuraScreenState.FULLSCREEN -> {
                isFullscreen = true
                setActionAndSystemBars(false)
                if (!event.isVertical) {
                    orientationEventListener?.enable()
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                }
            }

            LuraScreenState.WINDOWED -> {
                isFullscreen = false
                setActionAndSystemBars(true)
                orientationEventListener?.enable()
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            else -> {
                isFullscreen = false
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isInPictureInPicture) return

        currentOrientation = newConfig.orientation
        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                playerFragment?.setScreenState(LuraScreenState.WINDOWED)
            }

            Configuration.ORIENTATION_LANDSCAPE -> {
                playerFragment?.setScreenState(LuraScreenState.FULLSCREEN)
            }

            else -> {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.downloads_menu, menu)

        editButton = menu?.findItem(R.id.menu_item_edit)
        editButton?.isVisible = false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                supportFragmentManager.popBackStack()

                supportActionBar?.setDisplayShowHomeEnabled(false)
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
                return true
            }

            R.id.menu_item_edit -> {
                EventBus.getDefault().post(EditButtonPressedEvent())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // val count = supportFragmentManager.backStackEntryCount
        if (playerFragment != null) {
            if (isFullscreen) {
                playerFragment!!.setScreenState(LuraScreenState.WINDOWED)
                setActionAndSystemBars(true)
                return
            }
            destroyPlayerFragment()
            return
        }
        super.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        orientationEventListener?.disable()
        orientationEventListener = null
        super.onDestroy()
    }
}