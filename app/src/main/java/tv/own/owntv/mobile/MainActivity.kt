package tv.own.owntv.mobile

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import tv.own.owntv.core.i18n.AppLocale
import tv.own.owntv.core.i18n.LocaleStore
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.cast.CastController
import tv.own.owntv.mobile.playback.PipController
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.mobile.ui.components.MobileSheetHost
import tv.own.owntv.mobile.ui.shell.MobileShell
import tv.own.owntv.mobile.ui.theme.GlassBackdropRoot
import tv.own.owntv.mobile.ui.theme.MobileTheme

/**
 * The single activity the whole app runs in.
 *
 * A `FragmentActivity` rather than a bare `ComponentActivity` for exactly one reason: the Cast
 * button's device chooser is a dialog fragment, and it looks for a `FragmentManager` on whatever
 * activity hosts it. Nothing in this app draws a fragment; this is the base class the platform's own
 * cast picker requires in order to open at all.
 */
class MainActivity : FragmentActivity() {

    private val tuner: LiveTuner by inject()
    private val pip: PipController by inject()
    private val cast: CastController by inject()
    private val localeStore: LocaleStore by inject()
    private val settings: SettingsRepository by inject()

    private val player get() = tuner.player

    /** Both read on a lifecycle callback, where there is no time to suspend on a preference. */
    private var pipEnabled = true
    private var backgroundPlayback = true
    private var audioOnScreenOff = true
    private var pausedForBackground = false

    /** Set when *this* class dropped the picture on the way off screen, so returning restores it —
     *  and a sound-only mode the user chose themselves is left exactly as they left it. */
    private var droppedVideoForBackground = false

    /** Set the moment the floating window opens, and cleared by whichever comes first: coming back to
     *  full screen, or the window being closed — see [onPictureInPictureModeChanged]. */
    private var wasInPipWindow = false

    /** The result is deliberately ignored: refusing only costs the user the lockscreen controls, and
     *  playback must not depend on it. */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** The PiP window's own buttons. Registered only while the window is up. */
    private val pipActions = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(EXTRA_PIP_ACTION)) {
                PIP_TOGGLE -> player.togglePlayPause()
                PIP_BACK -> player.seekBy(-PIP_SEEK_MS)
                PIP_FORWARD -> player.seekBy(PIP_SEEK_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        askForNotifications()
        readOpenPlayerRequest(intent)
        // The buttons say Play or Pause depending on what is happening, so they are rebuilt whenever
        // that changes — a PiP window whose button lies is worse than one with no buttons.
        lifecycleScope.launch {
            player.isPlaying.collectLatest { if (pip.inPip.value) applyPipParams() }
        }
        lifecycleScope.launch { settings.pipEnabled.collect { pipEnabled = it } }
        lifecycleScope.launch { settings.backgroundPlayback.collect { backgroundPlayback = it } }
        lifecycleScope.launch { settings.audioOnScreenOff.collect { audioOnScreenOff = it } }
        keepScreenOnWhileThereIsAPicture()
        setContent {
            MobileTheme {
                // The wallpaper and its blurred copy sit outside the shell, so the frost every glass
                // panel samples is one image for the whole app rather than one per panel.
                GlassBackdropRoot {
                    // Inside the backdrop, because a sheet's whole reason for living in this window
                    // is that it can frost the same wallpaper everything else frosts.
                    MobileSheetHost {
                        // Width, not device type: a phone in landscape and a tablet in split-screen
                        // are the same problem, and the configuration re-reads itself on every
                        // rotation and resize.
                        MobileShell(windowWidthDp = LocalConfiguration.current.screenWidthDp)
                    }
                }
            }
        }
    }

    /**
     * Leaving the app while the picture is full screen takes the picture along, in the system's own
     * floating window.
     *
     * That window goes over *other* apps, so it belongs to leaving the app and to nothing else. The
     * button in the player's tools is the app's own mini player, which stays inside it. With the
     * setting off, or with no picture to carry, Home leaves the sound and the notification instead.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!pipEnabled) return
        if (!pip.playerOnScreen.value) return
        // Nothing to float while casting: the picture is on the television, and a little window here
        // would show a black rectangle with the receiver's transport buttons under it.
        if (cast.engine.value != null) return
        if (!player.isPlaying.value || player.audioOnly.value || player.audioOnlyMedia.value) return
        runCatching { enterPictureInPictureMode(pipParams()) }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pip.inPip.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            wasInPipWindow = true
            ContextCompat.registerReceiver(
                this,
                pipActions,
                IntentFilter(ACTION_PIP),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } else {
            runCatching { unregisterReceiver(pipActions) }
            // Deliberately no decision here. Leaving the window means one of two opposite things —
            // tapped, to come back to full screen, or closed — and this callback cannot tell them
            // apart. Nor can it be relied on to arrive first: on some builds it lands after onStop,
            // and a flag set here would then be read after the moment it was meant to answer. So the
            // two lifecycle callbacks decide, in whichever order they come: onResume means tapped,
            // onStop means closed.
            if (isFinishing) {
                wasInPipWindow = false
                closedThePipWindow()
            }
        }
    }

    /**
     * The X on the PiP window: the picture ends, the session does not.
     *
     * Closing the window says "off my screen", not "forget where I was". So the video stops, the sound
     * stops with it, and what stays behind is the notification and the quick-panel controls — press
     * play there and it comes back as sound only, tap the notification and the full player opens again
     * at the same place. Live channels included: the user closed a window, and stopping their
     * subscription's stream is a bigger thing than the button they pressed.
     */
    private fun closedThePipWindow() {
        if (!player.hasActiveStream) return
        // Sound-only first, then stop. The window is gone, so there is no surface and no reason to keep
        // a video decoder alive — and it settles what the play button in the quick panel will do next.
        //
        // Flagged as *this* class's doing, exactly as the screen-off path flags it, so coming back
        // undoes it. Without that, tapping the quick-panel controls opened the app with sound and a
        // black screen — and there is no full-screen sound-only mode in this app.
        if (!player.audioOnly.value) {
            droppedVideoForBackground = true
            player.enterAudioOnly()
        }
        if (player.isPlaying.value) player.togglePlayPause()
    }

    /** The playback notification was tapped. The player is a navigation destination, so the shell
     *  does the moving — see [PipController.openPlayerRequested]. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readOpenPlayerRequest(intent)
    }

    private fun readOpenPlayerRequest(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) {
            pip.openPlayerRequested.value = true
        }
    }

    /**
     * Off screen, drop the video decoder and keep the sound. A phone spends most of its life with the
     * screen off, and decoding frames nobody can see is the single most expensive thing this app can
     * do to a battery.
     *
     * Not on a rotation (the activity is only being rebuilt) and not in Picture-in-Picture, where the
     * window is still on screen.
     *
     * With background playback turned off the sound stops too — the stream is only paused, so coming
     * back resumes it where it was rather than reconnecting from the start.
     */
    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) return
        // Stopped after the floating window was up, without ever resuming in between: the window was
        // closed. That is its own answer, and not the background-playback question below.
        if (wasInPipWindow) {
            wasInPipWindow = false
            closedThePipWindow()
            return
        }
        if (pip.inPip.value) return
        if (!player.hasActiveStream) return
        if (!backgroundPlayback) {
            // Remembered, so a stream the user paused themselves is not resumed for them on return.
            if (player.isPlaying.value) {
                pausedForBackground = true
                player.togglePlayPause()
            }
            return
        }
        // Only when the user wants the picture dropped. With that switch off the stream keeps
        // decoding video nobody is looking at, which is their choice to make and costs battery.
        if (!audioOnScreenOff) return
        // Not if they are already in sound-only mode on purpose — coming back must not hand them a
        // picture they switched off themselves.
        if (player.audioOnly.value) return
        droppedVideoForBackground = true
        player.enterAudioOnly()
    }

    /** Back on screen at full size, so the window was tapped rather than closed. */
    override fun onResume() {
        super.onResume()
        wasInPipWindow = false
    }

    override fun onStart() {
        super.onStart()
        if (droppedVideoForBackground) {
            droppedVideoForBackground = false
            player.exitAudioOnly()
        }
        if (pausedForBackground) {
            pausedForBackground = false
            if (player.hasActiveStream && !player.isPlaying.value) player.togglePlayPause()
        }
    }

    /**
     * Nobody touches the screen during a film, and the phone's own timeout does not know that.
     *
     * Held on the window, by the activity, once — not by the composable that draws the picture. There
     * are three of those (the channel screen, the full screen player, the mini player) and they hand
     * the stream to one another: the arriving one would set the flag and the leaving one would then
     * clear it, which is exactly why the screen still went dark mid-film.
     */
    private fun keepScreenOnWhileThereIsAPicture() = lifecycleScope.launch {
        combine(player.isPlaying, player.audioOnly, player.audioOnlyMedia) { playing, off, radio ->
            playing && !off && !radio
        }.distinctUntilChanged().collect { keep ->
            if (keep) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun applyPipParams() {
        runCatching { setPictureInPictureParams(pipParams()) }
    }

    private fun pipParams(): PictureInPictureParams {
        val ctx = AppLocale.wrap(this, localeStore.currentTag.value)
        val playing = player.isPlaying.value
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(
                listOf(
                    pipAction(
                        android.R.drawable.ic_media_rew,
                        ctx.getString(R.string.player_skip_back),
                        PIP_BACK,
                    ),
                    pipAction(
                        if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                        ctx.getString(R.string.settings_remote_action_play_pause),
                        PIP_TOGGLE,
                    ),
                    pipAction(
                        android.R.drawable.ic_media_ff,
                        ctx.getString(R.string.player_skip_forward),
                        PIP_FORWARD,
                    ),
                ),
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(false)
        return builder.build()
    }

    private fun pipAction(icon: Int, label: String, action: String): RemoteAction = RemoteAction(
        Icon.createWithResource(this, icon),
        label,
        label,
        PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            Intent(ACTION_PIP).setPackage(packageName).putExtra(EXTRA_PIP_ACTION, action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ),
    )

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        /** Set on the playback notification's own intent: "open the player, not wherever the app was". */
        const val EXTRA_OPEN_PLAYER = "open_player"

        private const val ACTION_PIP = "tv.own.owntv.mobile.PIP"
        private const val EXTRA_PIP_ACTION = "pip_action"
        private const val PIP_TOGGLE = "toggle"
        private const val PIP_BACK = "back"
        private const val PIP_FORWARD = "forward"

        /** Fixed, not the user's seek step: three buttons is all a PiP window has room for, and a
         *  window is not where anyone sets up a 90-second jump. */
        private const val PIP_SEEK_MS = 10_000L
    }
}
