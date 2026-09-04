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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import tv.own.owntv.core.i18n.AppLocale
import tv.own.owntv.core.i18n.LocaleStore
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.playback.PipController
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.mobile.ui.shell.MobileShell
import tv.own.owntv.mobile.ui.theme.GlassBackdropRoot
import tv.own.owntv.mobile.ui.theme.MobileTheme

/** The single activity the whole app runs in. */
class MainActivity : ComponentActivity() {

    private val tuner: LiveTuner by inject()
    private val pip: PipController by inject()
    private val localeStore: LocaleStore by inject()
    private val settings: SettingsRepository by inject()

    private val player get() = tuner.player

    /** Both read on a lifecycle callback, where there is no time to suspend on a preference. */
    private var pipEnabled = true
    private var pipOnBack = false
    private var backgroundPlayback = true
    private var audioOnScreenOff = true
    private var pausedForBackground = false

    /** Set when *this* class dropped the picture on the way off screen, so returning restores it —
     *  and a sound-only mode the user chose themselves is left exactly as they left it. */
    private var droppedVideoForBackground = false

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
        // The buttons say Play or Pause depending on what is happening, so they are rebuilt whenever
        // that changes — a PiP window whose button lies is worse than one with no buttons.
        lifecycleScope.launch {
            player.isPlaying.collectLatest { if (pip.inPip.value) applyPipParams() }
        }
        lifecycleScope.launch { settings.pipEnabled.collect { pipEnabled = it } }
        lifecycleScope.launch { settings.pipOnBack.collect { pipOnBack = it } }
        lifecycleScope.launch { settings.backgroundPlayback.collect { backgroundPlayback = it } }
        lifecycleScope.launch { settings.audioOnScreenOff.collect { audioOnScreenOff = it } }
        keepScreenOnWhileThereIsAPicture()
        setContent {
            MobileTheme {
                // The wallpaper and its blurred copy sit outside the shell, so the frost every glass
                // panel samples is one image for the whole app rather than one per panel.
                GlassBackdropRoot {
                    // Width, not device type: a phone in landscape and a tablet in split-screen are
                    // the same problem, and the configuration re-reads itself on every rotation and
                    // resize.
                    MobileShell(windowWidthDp = LocalConfiguration.current.screenWidthDp)
                }
            }
        }
    }

    /**
     * Home pressed while watching: keep the picture in the little window rather than stopping it.
     * Only from the full screen player — from anywhere else the window would show the browsing UI
     * shrunk down, which is not what a PiP window is for.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipNow()
    }

    /**
     * Shrink into the little window if there is a picture worth keeping, and say whether it happened.
     *
     * Home asks unconditionally; Back asks only when the user turned "Picture-in-Picture on Back" on,
     * and uses the answer to decide whether it still has to close the player itself.
     */
    fun enterPipNow(fromBack: Boolean = false): Boolean {
        if (!pipEnabled) return false
        if (fromBack && !pipOnBack) return false
        if (!pip.playerOnScreen.value) return false
        if (!player.isPlaying.value || player.audioOnly.value || player.audioOnlyMedia.value) return false
        return runCatching { enterPictureInPictureMode(pipParams()) }.getOrDefault(false)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pip.inPip.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            ContextCompat.registerReceiver(
                this,
                pipActions,
                IntentFilter(ACTION_PIP),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } else {
            runCatching { unregisterReceiver(pipActions) }
            // The X on the PiP window closes the activity with it, and that is the one exit from PiP
            // that means "I am done" — tapping the window instead brings the app back, still playing.
            if (isFinishing) tuner.stop()
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

    private companion object {
        const val ACTION_PIP = "tv.own.owntv.mobile.PIP"
        const val EXTRA_PIP_ACTION = "pip_action"
        const val PIP_TOGGLE = "toggle"
        const val PIP_BACK = "back"
        const val PIP_FORWARD = "forward"

        /** Fixed, not the user's seek step: three buttons is all a PiP window has room for, and a
         *  window is not where anyone sets up a 90-second jump. */
        const val PIP_SEEK_MS = 10_000L
    }
}
