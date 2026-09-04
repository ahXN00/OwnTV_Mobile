package tv.own.owntv.mobile.ui.player

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.compose.koinInject
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.theme.AnimationLevel
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.playback.SleepTimer
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.player.OwnTVPlayer

/** What the sleep timer offers, in minutes. Round numbers, because nobody falls asleep to 37. */
private val SLEEP_MINUTES = intArrayOf(15, 30, 45, 60, 90)

/** The waveform's bars, and how tall each one gets at the top of its swing. */
private val WAVE_PEAKS = floatArrayOf(0.5f, 0.9f, 0.65f, 1f, 0.45f)
private val WAVE_HEIGHT = 44.dp
private val WAVE_BAR_WIDTH = 5.dp

/**
 * The screen for listening without watching.
 *
 * Dropping the picture is the single biggest thing a phone can do for its battery and its data
 * allowance, so it is a place to be rather than a warning to dismiss: the artwork, what is playing,
 * the volume, and a timer for the user who is falling asleep to it. The **Video** button brings the
 * picture straight back.
 *
 * It is told what to say rather than which tuner to ask, for the reason [MiniPlayer] is.
 */
@Composable
fun AudioOnlyStage(
    player: OwnTVPlayer,
    title: String,
    onBack: () -> Unit,
    /** Null for a stream that has no picture to come back to — a radio channel. */
    onShowVideo: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artworkUrl: String? = null,
    programmeEndMs: Long? = null,
    sleepTimer: SleepTimer = koinInject(),
    settings: SettingsRepository = koinInject(),
) {
    val volume by player.volume.collectAsStateWithLifecycle()
    val playing by player.isPlaying.collectAsStateWithLifecycle()
    val remaining by sleepTimer.remainingMs.collectAsStateWithLifecycle()
    val animations by settings.animationLevel.collectAsStateWithLifecycle(AnimationLevel.FULL)
    var timerSheet by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        IconButton(onClick = onBack, modifier = Modifier.padding(MobileDimens.GapSmall)) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = MobileDimens.ScreenPaddingH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(MobileDimens.SheetCorner))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                // A channel logo is the only artwork a live stream has, and plenty of them have none
                // at all — hence a glyph underneath rather than an empty square.
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(MobileDimens.GapMedium),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = MobileDimens.GapMedium),
            )
            subtitle?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Waveform(
                active = playing,
                animate = animations == AnimationLevel.FULL,
                modifier = Modifier.padding(top = MobileDimens.GapMedium),
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.player_audio_only_video_off)) },
                colors = AssistChipDefaults.assistChipColors(),
                modifier = Modifier.padding(top = MobileDimens.GapMedium),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = MobileDimens.GapLarge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { player.adjustVolumeByUser(it.toInt() - volume) },
                    // 150 % is the engine's own ceiling, and the boost quiet streams need.
                    valueRange = 0f..150f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MobileDimens.GapSmall),
                )
                Text(
                    text = stringResource(R.string.player_percent, volume),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                Modifier.padding(top = MobileDimens.GapMedium),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onShowVideo?.let { showVideo ->
                    FilledTonalButton(onClick = showVideo) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Text(
                            text = stringResource(R.string.player_audio_only_show_video),
                            modifier = Modifier.padding(start = MobileDimens.GapSmall),
                        )
                    }
                }
                TextButton(
                    onClick = { timerSheet = true },
                    modifier = Modifier.padding(start = MobileDimens.GapSmall),
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null)
                    Text(
                        text = remaining?.let {
                            stringResource(R.string.player_sleep_timer_remaining, minutesLabel(it))
                        } ?: stringResource(R.string.player_sleep_timer),
                        modifier = Modifier.padding(start = MobileDimens.GapSmall),
                    )
                }
                // Casting arrives with Plan 4 Phase 11A. The button is here and switched off rather
                // than absent, so the row does not change shape when it starts working.
                TextButton(onClick = {}, enabled = false) {
                    Icon(Icons.Default.Cast, contentDescription = null)
                    Text(
                        text = stringResource(R.string.common_cast),
                        modifier = Modifier.padding(start = MobileDimens.GapSmall),
                    )
                }
            }
        }
    }

    if (timerSheet) {
        SleepTimerSheet(programmeEndMs = programmeEndMs, onDismiss = { timerSheet = false })
    }
}

/** The sleep timer's own picker — reached from this screen and from the floating window's menu. */
@Composable
fun SleepTimerSheet(
    programmeEndMs: Long?,
    onDismiss: () -> Unit,
    sleepTimer: SleepTimer = koinInject(),
) {
    val remaining by sleepTimer.remainingMs.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_sleep_timer)) {
        if (remaining != null) {
            MobileListRow(
                title = stringResource(R.string.common_off),
                onClick = {
                    sleepTimer.cancel()
                    onDismiss()
                },
            )
        }
        SLEEP_MINUTES.forEach { minutes ->
            MobileListRow(
                title = stringResource(R.string.player_duration_minutes, minutes),
                onClick = {
                    sleepTimer.start(minutes * 60_000L)
                    onDismiss()
                },
            )
        }
        // Only with a guide behind it: "end of programme" with no programme is a button that stops
        // the stream at once.
        programmeEndMs?.let { endMs ->
            MobileListRow(
                title = stringResource(R.string.player_sleep_timer_end_of_programme),
                onClick = {
                    sleepTimer.start(endMs - System.currentTimeMillis())
                    onDismiss()
                },
            )
        }
    }
}

/** The countdown reads in whole minutes, rounded up: "Stops in 1 min" until it really is over. */
@Composable
private fun minutesLabel(remainingMs: Long): String {
    val minutes = ((remainingMs + 59_999L) / 60_000L).toInt()
    return stringResource(R.string.player_duration_minutes, minutes)
}

/** Something moving, so a screen with no picture still looks like it is playing. */
@Composable
private fun Waveform(active: Boolean, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier.height(WAVE_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WAVE_PEAKS.forEachIndexed { index, peak ->
            // Each bar swings on its own clock, or the row would pump as one block.
            val fraction = if (active && animate) {
                transition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = peak,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 420 + index * 90),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "bar$index",
                ).value
            } else {
                0.2f
            }
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .width(WAVE_BAR_WIDTH)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(WAVE_BAR_WIDTH / 2))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
