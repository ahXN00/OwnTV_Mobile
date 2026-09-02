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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.player.OwnTVPlayer

/** The pickers the tool bar opens. Each one is a sheet; each one also has a gesture. */
enum class PlayerSheet { VOLUME, BRIGHTNESS, SUBTITLES, AUDIO, ASPECT, SPEED, INFO, CHANNELS }

private val LiveRed = Color(0xFFE53935)

/**
 * Everything drawn over the picture: the title bar, the transport, the seek bar and the tool bar.
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
    visible: Boolean,
    isLive: Boolean,
    offsetSec: Int?,
    archiveWindowSec: Int,
    onBack: () -> Unit,
    onGoLive: () -> Unit,
    onScrubLive: (deltaSec: Int) -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onDock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .systemBarsPadding()
                    .padding(horizontal = MobileDimens.GapSmall, vertical = MobileDimens.GapTiny),
            ) {
                TopRow(player = player, title = title, subtitle = subtitle, onBack = onBack)
            }

            TransportRow(player = player, isLive = isLive, modifier = Modifier.align(Alignment.Center))

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .systemBarsPadding()
                    .padding(horizontal = MobileDimens.GapSmall),
            ) {
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
                ToolBar(player = player, isLive = isLive, onOpenSheet = onOpenSheet, onDock = onDock)
            }
        }
    }
}

@Composable
private fun TopRow(player: OwnTVPlayer, title: String, subtitle: String?, onBack: () -> Unit) {
    val engine by player.engineChip.collectAsStateWithLifecycle()
    val resolution by player.videoRes.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, stringResource(R.string.common_back), tint = Color.White)
        }
        Column(Modifier.weight(1f)) {
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
                    color = Color.White.copy(alpha = 0.75f),
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isLive) {
            RoundControl(Icons.Filled.FastRewind, R.string.player_skip_back) { player.seekBy(-step) }
        }
        RoundControl(
            icon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            labelRes = R.string.settings_remote_action_play_pause,
            size = 64.dp,
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
    size: androidx.compose.ui.unit.Dp = 52.dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.45f)),
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

    Column {
        Slider(
            value = fraction,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                dragging = false
                player.seekBy((dragValue * duration).toLong() - player.position.value)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
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
                color = Color.White.copy(alpha = 0.75f),
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

/** The tool bar: one button per picker, scrollable because a phone in portrait is narrow. */
@Composable
private fun ToolBar(
    player: OwnTVPlayer,
    isLive: Boolean,
    onOpenSheet: (PlayerSheet) -> Unit,
    onDock: () -> Unit,
) {
    val audioCount by player.audioCount.collectAsStateWithLifecycle()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tool(Icons.Filled.VolumeUp, R.string.player_tool_volume) { onOpenSheet(PlayerSheet.VOLUME) }
        Tool(Icons.Filled.BrightnessMedium, R.string.player_tool_brightness) { onOpenSheet(PlayerSheet.BRIGHTNESS) }
        Tool(Icons.Filled.ClosedCaption, R.string.player_tool_subtitles) { onOpenSheet(PlayerSheet.SUBTITLES) }
        if (audioCount > 1) {
            Tool(Icons.Filled.Audiotrack, R.string.player_tool_audio) { onOpenSheet(PlayerSheet.AUDIO) }
        }
        Tool(Icons.Filled.AspectRatio, R.string.player_tool_aspect) { onOpenSheet(PlayerSheet.ASPECT) }
        if (!isLive) {
            Tool(Icons.Filled.Speed, R.string.player_tool_speed) { onOpenSheet(PlayerSheet.SPEED) }
            // Live is already on the engine the television's compatibility mode switches TO, so the
            // toggle would have nothing to swap; for a film, an episode or a replay it is real.
            Tool(Icons.Filled.SwapHoriz, R.string.player_tool_engine) { player.toggleVodEngine() }
        }
        if (isLive) {
            Tool(Icons.Filled.FormatListBulleted, R.string.content_channel_overlay_title) {
                onOpenSheet(PlayerSheet.CHANNELS)
            }
        }
        Tool(Icons.Filled.Info, R.string.player_tool_info) { onOpenSheet(PlayerSheet.INFO) }
        Tool(Icons.Filled.PictureInPictureAlt, R.string.player_tool_mini, onClick = onDock)
    }
}

@Composable
private fun Tool(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .padding(vertical = MobileDimens.GapTiny),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
