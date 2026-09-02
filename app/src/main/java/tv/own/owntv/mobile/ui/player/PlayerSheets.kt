package tv.own.owntv.mobile.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.player.OwnTVPlayer
import tv.own.owntv.player.StreamInfoRow
import tv.own.owntv.player.ZoomMode
import tv.own.owntv.player.displayText
import tv.own.owntv.player.titleRes

/** Every picker the player's tool bar can open, chosen by [sheet]. */
@Composable
fun PlayerSheetHost(
    sheet: PlayerSheet,
    player: OwnTVPlayer,
    channels: List<ChannelEntity>,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    onPickChannel: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    when (sheet) {
        PlayerSheet.VOLUME -> VolumeSheet(player, onDismiss)
        PlayerSheet.BRIGHTNESS -> BrightnessSheet(brightness, onBrightness, onDismiss)
        PlayerSheet.SUBTITLES -> SubtitleSheet(player, onDismiss)
        PlayerSheet.AUDIO -> AudioSheet(player, onDismiss)
        PlayerSheet.ASPECT -> AspectSheet(player, onDismiss)
        PlayerSheet.SPEED -> SpeedSheet(player, onDismiss)
        PlayerSheet.INFO -> StreamInfoSheet(player, onDismiss)
        PlayerSheet.CHANNELS -> ChannelSheet(channels, onPickChannel, onDismiss)
    }
}

/** 0–150%: above 100 is the boost, the same range the television offers. */
@Composable
private fun VolumeSheet(player: OwnTVPlayer, onDismiss: () -> Unit) {
    val volume by player.volume.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_volume)) {
        SliderRow(
            value = volume / 150f,
            label = stringResource(R.string.player_percent, volume),
            onChange = { player.adjustVolumeByUser((it * 150).toInt() - player.volume.value) },
        )
        // The two-finger tap mutes as well; this row is the button every gesture must have.
        MobileListRow(
            title = stringResource(if (volume > 0) R.string.player_mute else R.string.player_unmute),
            onClick = { player.toggleMute() },
        )
    }
}

@Composable
private fun BrightnessSheet(brightness: Float, onBrightness: (Float) -> Unit, onDismiss: () -> Unit) {
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_brightness)) {
        SliderRow(
            value = brightness,
            label = stringResource(R.string.player_percent, (brightness * 100).toInt()),
            onChange = onBrightness,
        )
    }
}

@Composable
private fun SliderRow(value: Float, label: String, onChange: (Float) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapMedium),
    ) {
        Slider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SubtitleSheet(player: OwnTVPlayer, onDismiss: () -> Unit) {
    val tracks = remember { player.textTracks() }
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_subtitles)) {
        MobileListRow(
            title = stringResource(R.string.common_off),
            onClick = { player.disableSubtitles(); onDismiss() },
        )
        if (tracks.isEmpty()) {
            EmptyNote(stringResource(R.string.player_no_tracks))
        }
        tracks.forEach { track ->
            MobileListRow(
                title = track.label,
                subtitle = if (track.selected) stringResource(R.string.common_on) else null,
                onClick = { player.selectSubtitle(track.mpvId); onDismiss() },
            )
        }
    }
}

@Composable
private fun AudioSheet(player: OwnTVPlayer, onDismiss: () -> Unit) {
    val tracks = remember { player.audioTracks() }
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_audio_track)) {
        if (tracks.isEmpty()) EmptyNote(stringResource(R.string.player_no_tracks))
        tracks.forEach { track ->
            MobileListRow(
                title = track.label,
                subtitle = if (track.selected) stringResource(R.string.common_on) else null,
                onClick = { player.selectAudio(track.mpvId); onDismiss() },
            )
        }
    }
}

@Composable
private fun AspectSheet(player: OwnTVPlayer, onDismiss: () -> Unit) {
    val current by player.zoomMode.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_aspect)) {
        ZoomMode.entries.forEach { mode ->
            MobileListRow(
                title = stringResource(mode.labelRes),
                subtitle = if (mode == current) stringResource(R.string.common_on) else null,
                onClick = { player.setZoomModeByUser(mode); onDismiss() },
            )
        }
    }
}

private val SPEEDS = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)

@Composable
private fun SpeedSheet(player: OwnTVPlayer, onDismiss: () -> Unit) {
    val current by player.speed.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_speed)) {
        val locale = LocalConfiguration.current.locales[0]
        SPEEDS.forEach { speed ->
            val number = remember(speed, locale) {
                java.text.NumberFormat.getNumberInstance(locale).apply {
                    minimumFractionDigits = 1
                    maximumFractionDigits = 2
                }.format(speed)
            }
            MobileListRow(
                title = if (speed == 1.0) {
                    stringResource(R.string.player_speed_normal)
                } else {
                    stringResource(R.string.player_speed, number)
                },
                subtitle = if (speed == current) stringResource(R.string.common_on) else null,
                onClick = { player.setSpeed(speed); onDismiss() },
            )
        }
    }
}

/** The technical readout, rendered from core's own table so it matches the television's line for line. */
@Composable
private fun StreamInfoSheet(player: OwnTVPlayer, onDismiss: () -> Unit) {
    var rows by remember { mutableStateOf(emptyList<StreamInfoRow>()) }
    LaunchedEffect(player) { rows = player.streamInfo() }
    val res = LocalResources.current
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_stream_info)) {
        Column(
            Modifier
                .heightIn(max = (LocalConfiguration.current.screenHeightDp / 2).dp)
                .padding(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        text = stringResource(row.label.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.38f),
                    )
                    Text(
                        text = row.value.displayText(res),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.62f),
                    )
                }
            }
        }
    }
}

/** The other channels in this folder — the swipe-up list, and its button on the tool bar. */
@Composable
private fun ChannelSheet(
    channels: List<ChannelEntity>,
    onPick: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.content_channel_overlay_title),
    ) {
        LazyColumn(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp / 2).dp)) {
            items(channels, key = { it.id }) { channel ->
                MobileListRow(
                    title = channel.name,
                    subtitle = channel.number?.toString(),
                    onClick = { onPick(channel); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(MobileDimens.ScreenPaddingH),
    )
}
