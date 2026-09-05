package tv.own.owntv.mobile.ui.player

import tv.own.owntv.mobile.ui.components.MobileIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.core.live.EpgNowNext
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.theme.LocalAccentOnVideo
import tv.own.owntv.mobile.ui.theme.LocalMobileMotion
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.SquircleShape
import tv.own.owntv.player.LiveProgramme
import tv.own.owntv.player.OwnTVPlayer
import java.text.NumberFormat

/** The pickers the tool bar opens. Each one is a sheet; each one also has a gesture. */
enum class PlayerSheet { VOLUME, BRIGHTNESS, SUBTITLES, AUDIO, ASPECT, SPEED, INFO, CHANNELS }

/**
 * Everything drawn over the picture: the title dock, the transport capsule, the instrument and the
 * tools.
 *
 * It is one block that fades in and out together, so the picture is never half-covered, and every
 * control here is the visible twin of a gesture — the rule from the design is that no function is
 * reachable *only* by swiping.
 */
@Composable
fun PlayerControls(
    player: OwnTVPlayer,
    title: String,
    subtitle: String?,
    logoUrl: String?,
    visible: Boolean,
    isLive: Boolean,
    offsetSec: Int?,
    archiveWindowSec: Int,
    /** Now and Next for the channel playing, or null when its guide has nothing. */
    epg: EpgNowNext?,
    /** The wall-clock instant on screen while an archive plays; null at the live edge. */
    watchingWallMs: Long?,
    /** The channel's guide window, as the live timeline's boundary ticks. */
    timelineProgrammes: List<LiveProgramme>,
    /** The provider's own number for the channel, or null when the user has numbers turned off. */
    channelNumber: Int?,
    onBack: () -> Unit,
    onGoLive: () -> Unit,
    onScrubLive: (deltaSec: Int) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    /** Shrink into the app's own mini player, still playing. */
    onMini: () -> Unit,
    audioOnly: Boolean,
    onAudioOnly: () -> Unit,
    /** The screen-wide scrub gesture's running total, in media milliseconds, while it is happening. */
    gestureScrubMs: Long?,
    modifier: Modifier = Modifier,
) {
    // The app's own effects spring, not Material's default: with animations off it snaps, so the
    // chrome is simply there or not there.
    val fade = LocalMobileMotion.current.fast<Float>()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(fade),
        exit = fadeOut(fade),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(TopScrim)
                    .systemBarsPadding()
                    .padding(horizontal = MobileDimens.GapSmall, vertical = MobileDimens.GapTiny),
            ) {
                PlayerDock {
                    TopRow(
                        player = player,
                        title = title,
                        subtitle = subtitle,
                        logoUrl = logoUrl,
                        channelNumber = channelNumber,
                        showClock = isLive || watchingWallMs != null,
                        watchingWallMs = watchingWallMs,
                        onBack = onBack,
                    )
                    // Under the identity rather than beside it: the guide line is the longest text on
                    // the dock, and a phone has no width to spare on the row the title is already in.
                    MobileNowNextCard(
                        epg = epg,
                        atMs = watchingWallMs,
                        modifier = Modifier.padding(top = MobileDimens.GapTiny),
                    )
                }
            }

            TransportRow(player = player, isLive = isLive, modifier = Modifier.align(Alignment.Center))

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(BottomScrim)
                    .systemBarsPadding()
                    .padding(horizontal = MobileDimens.GapSmall, vertical = MobileDimens.GapTiny),
            ) {
                PlayerDock {
                    if (isLive) {
                        LiveBar(
                            offsetSec = offsetSec,
                            archiveWindowSec = archiveWindowSec,
                            programmes = timelineProgrammes,
                            onScrubLive = onScrubLive,
                        )
                    } else {
                        SeekBar(player, gestureScrubMs)
                    }
                    ToolBar(
                        player = player,
                        isLive = isLive,
                        goLive = if (isLive && (offsetSec ?: 0) > 1) onGoLive else null,
                        onOpenSheet = onOpenSheet,
                        onMini = onMini,
                        audioOnly = audioOnly,
                        onAudioOnly = onAudioOnly,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopRow(
    player: OwnTVPlayer,
    title: String,
    subtitle: String?,
    logoUrl: String?,
    channelNumber: Int?,
    showClock: Boolean,
    watchingWallMs: Long?,
    onBack: () -> Unit,
) {
    val engine by player.engineChip.collectAsStateWithLifecycle()
    val resolution by player.videoRes.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(MobileIcons.ArrowBack, stringResource(R.string.common_back), tint = Color.White)
        }
        ChannelLogo(logoUrl = logoUrl, title = title, number = channelNumber)
        Column(Modifier.weight(1f).padding(start = MobileDimens.GapSmall)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(subtitle, engine, resolution)
                .joinToString(stringResource(R.string.player_metadata_separator))
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnVideo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showClock) {
            MobilePlayerClock(
                watchingMs = watchingWallMs,
                modifier = Modifier.padding(horizontal = MobileDimens.GapTiny),
            )
        }
        // Cast belongs to Phase 5, when there is a session to hand over. The slot is here so the bar
        // does not shift sideways the day it starts working.
        IconButton(onClick = { }, enabled = false) {
            Icon(MobileIcons.Cast, stringResource(R.string.common_cast), tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun TransportRow(player: OwnTVPlayer, isLive: Boolean, modifier: Modifier = Modifier) {
    val playing by player.isPlaying.collectAsStateWithLifecycle()
    val step by player.seekStepMs.collectAsStateWithLifecycle()
    TransportCapsule(modifier) {
        if (!isLive) {
            RoundControl(MobileIcons.FastRewind, R.string.player_skip_back) { player.seekBy(-step) }
        }
        RoundControl(
            icon = if (playing) MobileIcons.Pause else MobileIcons.PlayArrow,
            labelRes = R.string.settings_remote_action_play_pause,
            size = 68.dp,
            onClick = { player.togglePlayPause() },
        )
        if (!isLive) {
            RoundControl(MobileIcons.FastForward, R.string.player_skip_forward) { player.seekBy(step) }
        }
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    labelRes: Int,
    // 48 dp, not the television's 44: this one is hit with a thumb.
    size: Dp = 48.dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size).clip(SquircleShape(size / 2)),
    ) {
        Icon(icon, stringResource(labelRes), tint = Color.White, modifier = Modifier.size(size / 2))
    }
}

/**
 * Position, duration and the scrubber, for anything with an end: a film, an episode, a replay.
 *
 * The bar is dragged either directly or by the screen-wide horizontal gesture, and both show the same
 * bubble in the same place — [gestureDeltaMs] is the gesture's running total, still unapplied.
 */
@Composable
private fun SeekBar(player: OwnTVPlayer, gestureDeltaMs: Long?) {
    val position by player.position.collectAsStateWithLifecycle()
    val duration by player.duration.collectAsStateWithLifecycle()
    val buffered by player.bufferedMs.collectAsStateWithLifecycle()
    if (duration <= 0) return
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val playedFraction = (position.toFloat() / duration).coerceIn(0f, 1f)
    val gestureFraction = gestureDeltaMs?.let {
        ((position + it).toFloat() / duration).coerceIn(0f, 1f)
    }
    val fraction = dragValue.takeIf { dragging } ?: gestureFraction ?: playedFraction
    val bufferedFraction = (buffered.toFloat() / duration).coerceIn(fraction, 1f)
    val targetMs = (fraction * duration).toLong()
    // Never the theme accent: over a picture the light theme's accent is a dark tone on a dark scene.
    val accent = LocalAccentOnVideo.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        TimeCap(formatTimestamp(targetMs), Alignment.Start)
        MobileSeekBar(
            fraction = fraction,
            bufferedFraction = bufferedFraction,
            dragging = dragging || gestureDeltaMs != null,
            accent = accent,
            // While the value is moving, say where it will land and by how much.
            bubbleTargetMs = targetMs.takeIf { dragging || gestureDeltaMs != null },
            bubbleDeltaMs = gestureDeltaMs ?: (targetMs - position).takeIf { dragging },
            onSeekTo = { player.seekBy((it * duration).toLong() - player.position.value) },
            onDrag = { dragging = true; dragValue = it },
            onDragEnd = {
                dragging = false
                player.seekBy((dragValue * duration).toLong() - player.position.value)
            },
            modifier = Modifier.weight(1f).padding(horizontal = MobileDimens.GapSmall),
        )
        TimeCap(stringResource(R.string.player_time_remaining, formatTimestamp(duration - targetMs)), Alignment.End)
    }
}

/**
 * The live edge, and how far back from it the picture is.
 *
 * The timeline is programme-aware: it spans the last two hours up to now, marks where each programme
 * began, and names the one under the finger. A channel whose provider keeps no archive has nothing
 * to drag into, so it gets the badge alone rather than a bar that refuses to move.
 */
@Composable
private fun LiveBar(
    offsetSec: Int?,
    archiveWindowSec: Int,
    programmes: List<LiveProgramme>,
    onScrubLive: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (archiveWindowSec > 0) {
            MobileLiveTimeline(
                offsetSec = offsetSec ?: 0,
                programmes = programmes,
                // Now. Read per composition, exactly as the television reads it — the bar's ticks are
                // recomputed on the minute, not on the second.
                liveEdgeMs = System.currentTimeMillis(),
                accent = LocalAccentOnVideo.current,
                onScrub = onScrubLive,
                modifier = Modifier.weight(1f).padding(end = MobileDimens.GapSmall),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        LiveStateBadge(offsetSec)
    }
}

/**
 * The tools: one button per picker, scrollable because a phone in portrait is narrow.
 *
 * Each is a 48 dp square that grows into its name when held. The three with no gesture twin —
 * subtitles, aspect and dropping the picture — are pinned open instead, because a control nobody can
 * find is not a control. Volume, brightness and speed all have a finger gesture already.
 */
@Composable
private fun ToolBar(
    player: OwnTVPlayer,
    isLive: Boolean,
    /** The way back to the live edge, or null when the picture is already there. */
    goLive: (() -> Unit)?,
    onOpenSheet: (PlayerSheet) -> Unit,
    /** Shrink into the app's own mini player, still playing. */
    onMini: () -> Unit,
    audioOnly: Boolean,
    onAudioOnly: () -> Unit,
) {
    val audioCount by player.audioCount.collectAsStateWithLifecycle()
    val speed by player.speed.collectAsStateWithLifecycle()
    val engine by player.engineChip.collectAsStateWithLifecycle()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapTiny),
    ) {
        // First, so the way back to now is the first thing the thumb reaches on a bar that scrolls.
        GoLivePill(enabled = goLive != null, onClick = { goLive?.invoke() })
        CtrlButton(MobileIcons.VolumeUp, stringResource(R.string.player_tool_volume), {
            onOpenSheet(PlayerSheet.VOLUME)
        })
        CtrlButton(MobileIcons.BrightnessMedium, stringResource(R.string.player_tool_brightness), {
            onOpenSheet(PlayerSheet.BRIGHTNESS)
        })
        CtrlButton(
            icon = MobileIcons.ClosedCaption,
            label = stringResource(R.string.player_tool_subtitles),
            onClick = { onOpenSheet(PlayerSheet.SUBTITLES) },
            pinned = true,
        )
        if (audioCount > 1) {
            CtrlButton(MobileIcons.Audiotrack, stringResource(R.string.player_tool_audio), {
                onOpenSheet(PlayerSheet.AUDIO)
            })
        }
        CtrlButton(
            icon = MobileIcons.AspectRatio,
            label = stringResource(R.string.player_tool_aspect),
            onClick = { onOpenSheet(PlayerSheet.ASPECT) },
            pinned = true,
        )
        if (!isLive) {
            SpeedButton(
                rate = formatSpeed(speed),
                label = stringResource(R.string.player_tool_speed),
                active = speed != 1.0,
                onClick = { onOpenSheet(PlayerSheet.SPEED) },
            )
            // Live is already on the engine the television's compatibility mode switches TO, so the
            // toggle would have nothing to swap; for a film, an episode or a replay it is real.
            EngineToggle(
                engine = engine.orEmpty(),
                label = stringResource(R.string.player_tool_engine),
                // ExoPlayer is not this app's default for a film, so being on it is a state worth
                // colouring — it is what the user switched to.
                active = engine == EXO,
                icon = MobileIcons.SwapHoriz,
                onClick = { player.toggleVodEngine() },
            )
        }
        if (isLive) {
            CtrlButton(MobileIcons.FormatListBulleted, stringResource(R.string.content_channel_overlay_title), {
                onOpenSheet(PlayerSheet.CHANNELS)
            })
        }
        CtrlButton(MobileIcons.Info, stringResource(R.string.player_tool_info), {
            onOpenSheet(PlayerSheet.INFO)
        })
        // Dropping the picture is the phone's biggest battery and data saving, so it is a button on
        // the bar rather than something only the notification offers.
        CtrlButton(
            icon = MobileIcons.MusicNote,
            label = stringResource(R.string.player_tool_audio_only),
            onClick = onAudioOnly,
            active = audioOnly,
            pinned = true,
        )
        // Shrink into the app's own small player and keep browsing. Not the system's floating window:
        // that one goes over *other* apps and is what pressing Home gives, so it is not a button.
        CtrlButton(MobileIcons.PictureInPictureAlt, stringResource(R.string.settings_mini_player), onMini)
    }
}

/** The engine chip's own name for ExoPlayer, as the player publishes it. */
private const val EXO = "EXO"

/** "Normal" at 1x, "1.5x" otherwise — the same wording the television uses. */
@Composable
internal fun formatSpeed(speed: Double): String {
    if (speed == 1.0) return stringResource(R.string.player_speed_normal_short)
    val locale = LocalConfiguration.current.locales[0]
    val number = remember(speed, locale) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 2
        }.format(speed)
    }
    return stringResource(R.string.player_speed, number)
}

/** 0:42 / 23:45 / 1:23:45 — the app's one duration format, the same one the television uses. */
@Composable
fun formatTimestamp(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) {
        stringResource(R.string.common_timestamp_hours, h, m, s)
    } else {
        stringResource(R.string.common_timestamp_minutes, m, s)
    }
}
