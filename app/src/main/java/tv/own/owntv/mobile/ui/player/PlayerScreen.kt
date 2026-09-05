package tv.own.owntv.mobile.ui.player

import android.content.pm.ActivityInfo
import android.os.Build
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import tv.own.owntv.mobile.MainActivity
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.playback.PipController
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.ui.screens.library.VodTuner
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.player.PlaybackFailure
import tv.own.owntv.player.ZoomMode
import tv.own.owntv.player.describe

private const val CONTROLS_TIMEOUT_MS = 3_000L
private const val HUD_TIMEOUT_MS = 900L
private const val SPEED_HOLD = 2.0

/**
 * The picture, full screen, with everything on top of it.
 *
 * It is a route rather than its own activity, so the stream it shows is the one [LiveTuner] already
 * has running — going full screen and coming back out is a change of view, never a restart. Back and
 * the swipe down both leave it playing, which is what puts it into the mini player.
 *
 * The screen asks for landscape when it opens and then lets go of the orientation again, so a video
 * lands the right way up without a user who prefers portrait being trapped in landscape.
 */
@Composable
fun PlayerScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    tuner: LiveTuner = koinInject(),
    vodTuner: VodTuner = koinInject(),
    pip: PipController = koinInject(),
    settings: SettingsRepository = koinInject(),
) {
    val player = tuner.player
    val activity = LocalActivity.current
    val inPip by pip.inPip.collectAsStateWithLifecycle()
    val res = LocalResources.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val channel by tuner.channel.collectAsStateWithLifecycle()
    val film by vodTuner.playing.collectAsStateWithLifecycle()
    val nowNext by tuner.nowNext.collectAsStateWithLifecycle()
    val siblings by tuner.siblings.collectAsStateWithLifecycle()
    val offsetSec by tuner.offsetSec.collectAsStateWithLifecycle()
    val duration by player.duration.collectAsStateWithLifecycle()
    val error by player.error.collectAsStateWithLifecycle()
    val errorInfo by player.errorInfo.collectAsStateWithLifecycle()
    val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
    // Either the user turned the picture off, or the stream never had one (a radio channel).
    val audioOnly by player.audioOnly.collectAsStateWithLifecycle()
    val audioOnlyMedia by player.audioOnlyMedia.collectAsStateWithLifecycle()
    // How far a value moves per centimetre of finger. 100 is the untouched behaviour.
    val gestureSensitivity by settings.gestureSensitivityPct.collectAsStateWithLifecycle(100)

    // A replay has an end and therefore a seek bar; the live edge and a rewind into the archive have
    // neither, and get the red bar instead.
    val isLive = channel != null && (offsetSec != null || duration <= 0L)

    var controlsVisible by remember { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var brightness by remember { mutableFloatStateOf(0.5f) }
    var hud by remember { mutableStateOf<String?>(null) }
    // Fractional carry: one flick of a thumb is many tiny deltas, and rounding each one to a whole
    // percent on its own would throw most of the movement away.
    val carry = remember { Carry() }

    // Full screen means full screen: no status bar, no navigation bar. The display is kept awake by
    // VideoStage, which knows whether there is a picture to stay awake for.
    //
    // This is also where the activity learns that the picture is on screen, which is the difference
    // between "home was pressed while watching" (Picture-in-Picture) and "home was pressed" (leave).
    DisposableEffect(activity) {
        val window = activity?.window
        val insets = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insets?.hide(WindowInsetsCompat.Type.systemBars())
        // Hiding the bars is not enough: the strip the camera sits in stays outside the window
        // unless the window is told to lay out into it, and the wallpaper shows through there.
        val cutoutMode = window?.attributes?.layoutInDisplayCutoutMode
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    // Before Android 11 only the short edges can be drawn into, which is the phone's
                    // top in portrait — the one that matters on a notched device.
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        pip.playerOnScreen.value = true
        onDispose {
            pip.playerOnScreen.value = false
            insets?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (window != null) {
                window.attributes = window.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    if (cutoutMode != null) layoutInDisplayCutoutMode = cutoutMode
                }
            }
        }
    }
    // Once the rotation has actually happened, hand the orientation back: the request above was to
    // arrive in landscape, not to stay there.
    LaunchedEffect(landscape) {
        if (landscape) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun setBrightness(value: Float) {
        brightness = value.coerceIn(0.01f, 1f)
        val window = activity?.window ?: return
        window.attributes = window.attributes.apply { screenBrightness = brightness }
    }

    fun showHud(text: String) {
        hud = text
    }

    LaunchedEffect(hud) {
        if (hud != null) {
            delay(HUD_TIMEOUT_MS)
            hud = null
        }
    }
    // The controls go away on their own, but never while a picker is open over them.
    LaunchedEffect(controlsVisible, sheet, isPlaying) {
        if (controlsVisible && sheet == null && isPlaying) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    // Back leaves the player playing — into the little window over whatever the user goes to next if
    // they asked for that, into the mini player otherwise.
    BackHandler {
        if ((activity as? MainActivity)?.enterPipNow(fromBack = true) != true) onExit()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .playerGestures(
                onTap = { controlsVisible = !controlsVisible },
                onDoubleTapLeft = { skip(tuner, isLive, forward = false) },
                onDoubleTapRight = { skip(tuner, isLive, forward = true) },
                onScrub = { carry.scrub += it },
                onScrubEnd = {
                    if (isLive) {
                        val window = tuner.archiveWindowSec()
                        if (window > 0) tuner.scrubLive((-carry.scrub * window).toInt())
                    } else if (duration > 0) {
                        player.seekBy((carry.scrub * duration).toLong())
                    }
                    carry.scrub = 0f
                },
                onBrightness = { delta ->
                    setBrightness(brightness + delta)
                    showHud(res.getString(R.string.player_percent, (brightness * 100).toInt()))
                },
                onVolume = { delta ->
                    carry.volume += delta * 150f
                    val whole = carry.volume.toInt()
                    if (whole != 0) {
                        carry.volume -= whole
                        player.adjustVolumeByUser(whole)
                    }
                    showHud(res.getString(R.string.player_percent, player.volume.value))
                },
                onPinch = { zoomIn ->
                    player.setZoomModeByUser(if (zoomIn) ZoomMode.FILL else ZoomMode.FIT)
                },
                onSwipeDown = onExit,
                onSwipeUp = { if (isLive) sheet = PlayerSheet.CHANNELS },
                onSpeedHold = { held ->
                    if (isLive) return@playerGestures
                    if (held) {
                        carry.speedBefore = player.speed.value
                        player.setSpeed(SPEED_HOLD)
                    } else {
                        player.setSpeed(carry.speedBefore)
                    }
                },
                onTwoFingerTap = { player.toggleMute() },
                sensitivity = gestureSensitivity / 100f,
            ),
    ) {
        VideoStage(player = player, modifier = Modifier.fillMaxSize())

        // No picture: the player stays exactly as it is and the rectangle it would fill shows what is
        // playing instead. Not a screen of its own — the controls and the gestures are still these.
        val noPicture = (audioOnly || audioOnlyMedia) && !inPip
        if (noPicture) {
            AudioOnlyBackdrop(
                title = channel?.name ?: film?.title.orEmpty(),
                playing = isPlaying,
                compact = controlsVisible,
                subtitle = if (channel != null) nowNext?.now?.title else film?.subtitle,
                artworkUrl = channel?.logoUrl ?: film?.posterUrl,
                programmeEndMs = nowNext?.now?.stopMs,
            )
        }

        val failure = error
        if (failure != null) {
            ErrorPanel(failure = failure, detailRes = errorInfo?.reason?.messageRes, onRetry = player::retry)
        } else if (!isPlaying && !noPicture) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        PlayerControls(
            player = player,
            // Whichever tuner has the surface. Only one of them ever does.
            title = channel?.name ?: film?.title.orEmpty(),
            subtitle = if (channel != null) nowNext?.now?.title else film?.subtitle,
            logoUrl = channel?.logoUrl ?: film?.posterUrl,
            // Nothing is drawn over the picture in the little window: it is a thumbnail, and the
            // system draws its own buttons on top of it.
            visible = controlsVisible && !inPip,
            isLive = isLive,
            offsetSec = offsetSec,
            archiveWindowSec = tuner.archiveWindowSec(),
            onBack = onExit,
            onGoLive = tuner::goToLive,
            onScrubLive = tuner::scrubLive,
            onOpenSheet = { sheet = it },
            onDock = onExit,
            // A stream with no video track has nothing to go back to, so for that one the button
            // only ever reports the state it is already in.
            audioOnly = audioOnly || audioOnlyMedia,
            onAudioOnly = { if (!audioOnlyMedia) tuner.setAudioOnly(!audioOnly) },
        )

        hud.takeIf { !inPip }?.let { text ->
            PlayerToast(Modifier.align(Alignment.Center)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
        }
    }

    sheet.takeIf { !inPip }?.let { open ->
        PlayerSheetHost(
            sheet = open,
            player = player,
            channels = siblings,
            brightness = brightness,
            onBrightness = { setBrightness(it) },
            onPickChannel = { tuner.switchTo(it) },
            onDismiss = { sheet = null },
        )
    }
}

/** Whatever a double tap means here: the user's own skip step, or the same step of archive. */
private fun skip(tuner: LiveTuner, isLive: Boolean, forward: Boolean) {
    val step = tuner.player.seekStepMs.value
    if (isLive) {
        val seconds = (step / 1000).toInt().coerceAtLeast(1)
        tuner.scrubLive(if (forward) -seconds else seconds)
    } else {
        tuner.player.seekBy(if (forward) step else -step)
    }
}

@Composable
private fun ErrorPanel(failure: PlaybackFailure, detailRes: Int?, onRetry: () -> Unit) {
    val res = LocalResources.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PlayerToast {
            Text(
                // Core owns the wording, so the phone and the television explain a failure alike.
                text = failure.describe { id, args -> res.getString(id, *args.toTypedArray()) },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (detailRes != null) {
                Text(
                    text = stringResource(detailRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.common_retry)) }
        }
    }
}

/** The leftovers of a gesture that has not yet added up to a whole step. */
private class Carry {
    var scrub = 0f
    var volume = 0f
    var speedBefore = 1.0
}
