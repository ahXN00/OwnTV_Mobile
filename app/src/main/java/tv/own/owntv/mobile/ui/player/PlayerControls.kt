package tv.own.owntv.mobile.ui.player

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.theme.LocalAccentOnVideo
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.SquircleShape
import tv.own.owntv.player.OwnTVPlayer
import java.text.NumberFormat

/** The pickers the tool bar opens. Each one is a sheet; each one also has a gesture. */
enum class PlayerSheet { VOLUME, BRIGHTNESS, SUBTITLES, AUDIO, ASPECT, SPEED, INFO, CHANNELS }

private val LiveRed = Color(0xFFE53935)

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
    onBack: () -> Unit,
    onGoLive: () -> Unit,
    onScrubLive: (deltaSec: Int) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onDock: () -> Unit,
    onAudioOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
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
                        onBack = onBack,
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
                            onGoLive = onGoLive,
                            onScrubLive = onScrubLive,
                        )
                    } else {
                        SeekBar(player)
                    }
                    ToolBar(
                        player = player,
                        isLive = isLive,
                        onOpenSheet = onOpenSheet,
                        onDock = onDock,
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
    onBack: () -> Unit,
) {
    val engine by player.engineChip.collectAsStateWithLifecycle()
    val resolution by player.videoRes.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, stringResource(R.string.common_back), tint = Color.White)
        }
        ChannelLogo(logoUrl = logoUrl, title = title)
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
        // Cast belongs to Phase 5, when there is a session to hand over. The slot is here so the bar
        // does not shift sideways the day it starts working.
        IconButton(onClick = { }, enabled = false) {
            Icon(Icons.Filled.Cast, stringResource(R.string.common_cast), tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun TransportRow(player: OwnTVPlayer, isLive: Boolean, modifier: Modifier = Modifier) {
    val playing by player.isPlaying.collectAsStateWithLifecycle()
    val step by player.seekStepMs.collectAsStateWithLifecycle()
    TransportCapsule(modifier) {
        if (!isLive) {
            RoundControl(Icons.Filled.FastRewind, R.string.player_skip_back) { player.seekBy(-step) }
        }
        RoundControl(
            icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            labelRes = R.string.settings_remote_action_play_pause,
            size = 68.dp,
            onClick = { player.togglePlayPause() },
        )
        if (!isLive) {
            RoundControl(Icons.Filled.FastForward, R.string.player_skip_forward) { player.seekBy(step) }
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

/** Position, duration and the scrubber, for anything with an end: a film, an episode, a replay. */
@Composable
private fun SeekBar(player: OwnTVPlayer) {
    val position by player.position.collectAsStateWithLifecycle()
    val duration by player.duration.collectAsStateWithLifecycle()
    if (duration <= 0) return
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val fraction = if (dragging) dragValue else (position.toFloat() / duration).coerceIn(0f, 1f)
    // Never the theme accent: over a picture the light theme's accent is a dark tone on a dark scene.
    val accent = LocalAccentOnVideo.current

    Column {
        Slider(
            value = fraction,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                dragging = false
                player.seekBy((dragValue * duration).toLong() - player.position.value)
            },
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(
                    R.string.player_time_progress,
                    formatTimestamp((fraction * duration).toLong()),
                    formatTimestamp(duration),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.player_time_remaining, formatTimestamp(duration - (fraction * duration).toLong())),
                style = MaterialTheme.typography.labelMedium,
                color = OnVideo,
            )
        }
    }
}

/**
 * The live edge, and how far back from it the picture is.
 *
 * The bar spans the provider's whole archive window, so dragging it left walks back into yesterday
 * and letting go loads that instant; the right-hand end is now. The pill returns to the edge.
 */
@Composable
private fun LiveBar(
    offsetSec: Int?,
    archiveWindowSec: Int,
    onGoLive: () -> Unit,
    onScrubLive: (Int) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(1f) }
    val atEdge = offsetSec == null
    val fraction = when {
        dragging -> dragValue
        archiveWindowSec <= 0 || offsetSec == null -> 1f
        else -> (1f - offsetSec.toFloat() / archiveWindowSec).coerceIn(0f, 1f)
    }

    Column {
        if (archiveWindowSec > 0) {
            Slider(
                value = fraction,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    dragging = false
                    val target = ((1f - dragValue) * archiveWindowSec).toInt()
                    onScrubLive(target - (offsetSec ?: 0))
                },
                colors = SliderDefaults.colors(
                    thumbColor = LiveRed,
                    activeTrackColor = LiveRed,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (atEdge) {
                    stringResource(R.string.player_at_live_edge)
                } else {
                    stringResource(R.string.player_behind_live, formatTimestamp(offsetSec * 1000L))
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (atEdge) LiveRed else Color.White,
            )
            Spacer(Modifier.weight(1f))
            if (!atEdge) {
                TextButton(onClick = onGoLive) {
                    Text(stringResource(R.string.player_go_live), color = LiveRed)
                }
            }
        }
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
    onOpenSheet: (PlayerSheet) -> Unit,
    onDock: () -> Unit,
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
        CtrlButton(Icons.Filled.VolumeUp, stringResource(R.string.player_tool_volume), {
            onOpenSheet(PlayerSheet.VOLUME)
        })
        CtrlButton(Icons.Filled.BrightnessMedium, stringResource(R.string.player_tool_brightness), {
            onOpenSheet(PlayerSheet.BRIGHTNESS)
        })
        CtrlButton(
            icon = Icons.Filled.ClosedCaption,
            label = stringResource(R.string.player_tool_subtitles),
            onClick = { onOpenSheet(PlayerSheet.SUBTITLES) },
            pinned = true,
        )
        if (audioCount > 1) {
            CtrlButton(Icons.Filled.Audiotrack, stringResource(R.string.player_tool_audio), {
                onOpenSheet(PlayerSheet.AUDIO)
            })
        }
        CtrlButton(
            icon = Icons.Filled.AspectRatio,
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
                icon = Icons.Filled.SwapHoriz,
                onClick = { player.toggleVodEngine() },
            )
        }
        if (isLive) {
            CtrlButton(Icons.Filled.FormatListBulleted, stringResource(R.string.content_channel_overlay_title), {
                onOpenSheet(PlayerSheet.CHANNELS)
            })
        }
        CtrlButton(Icons.Filled.Info, stringResource(R.string.player_tool_info), {
            onOpenSheet(PlayerSheet.INFO)
        })
        // Dropping the picture is the phone's biggest battery and data saving, so it is a button on
        // the bar rather than something only the notification offers.
        CtrlButton(
            icon = Icons.Filled.MusicNote,
            label = stringResource(R.string.player_tool_audio_only),
            onClick = onAudioOnly,
            pinned = true,
        )
        CtrlButton(Icons.Filled.PictureInPictureAlt, stringResource(R.string.player_tool_mini), onDock)
    }
}

/** The engine chip's own name for ExoPlayer, as the player publishes it. */
private const val EXO = "EXO"

/** "Normal" at 1x, "1.5x" otherwise — the same wording the television uses. */
@Composable
private fun formatSpeed(speed: Double): String {
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
