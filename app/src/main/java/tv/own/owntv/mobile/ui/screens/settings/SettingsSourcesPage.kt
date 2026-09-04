package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * Where the content comes from: the playlists, the guide feeds that fill the EPG, and the two clock
 * settings that decide whether a programme's times land where the provider meant them to.
 *
 * The two lists are pages of their own — a playlist row carries badges, live import counts and six
 * actions, and neither list fits above the other on a phone.
 */
@Composable
fun SettingsSourcesPage(
    onOpenLeaf: (SettingsLeaf) -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    var epgOffsetSheet by remember { mutableStateOf(false) }
    var catchupSheet by remember { mutableStateOf(false) }

    SettingsPage(modifier) {
        settingsLeafRows(SettingsGroup.SOURCES, onOpenLeaf)

        settingsSection(R.string.content_epg)
        item(key = "epg-offset") {
            SettingRow(
                title = stringResource(R.string.content_epg_time_offset),
                subtitle = stringResource(R.string.settings_epg_offset_root_description),
                value = utcOffsetLabel(vm.settings.epgOffsetMinutes.pref(0)),
                onClick = { epgOffsetSheet = true },
            )
        }
        item(key = "catchup") {
            val tz = vm.settings.catchupTimezone.pref(SettingsRepository.CatchupTimezone.DEVICE)
            SettingRow(
                title = stringResource(R.string.settings_catchup),
                subtitle = stringResource(R.string.settings_catchup_description),
                value = if (tz == SettingsRepository.CatchupTimezone.DEVICE) {
                    stringResource(R.string.settings_catchup_timezone_device)
                } else {
                    utcOffsetLabel(vm.settings.catchupOffsetMinutes.pref(0))
                },
                onClick = { catchupSheet = true },
            )
        }
    }

    if (epgOffsetSheet) {
        val offset = vm.settings.epgOffsetMinutes.pref(0)
        MobileBottomSheet(
            onDismissRequest = { epgOffsetSheet = false },
            title = stringResource(R.string.content_epg_time_offset),
        ) {
            Text(
                text = stringResource(R.string.settings_epg_offset_dialog_description),
                modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
            )
            OffsetStepper(
                minutes = offset,
                range = -12 * 60..14 * 60,
                onChange = { vm.edit { setEpgOffsetMinutes(it) } },
            )
        }
    }

    if (catchupSheet) {
        val tz = vm.settings.catchupTimezone.pref(SettingsRepository.CatchupTimezone.DEVICE)
        val offset = vm.settings.catchupOffsetMinutes.pref(0)
        val player = vm.settings.catchupPlayer.pref(SettingsRepository.CatchupPlayer.INTERNAL)
        MobileBottomSheet(
            onDismissRequest = { catchupSheet = false },
            title = stringResource(R.string.settings_catchup),
        ) {
            SettingRow(
                title = stringResource(R.string.settings_catchup_timezone_device),
                checked = tz == SettingsRepository.CatchupTimezone.DEVICE,
                onCheckedChange = { device ->
                    vm.edit {
                        setCatchupTimezone(
                            if (device) SettingsRepository.CatchupTimezone.DEVICE
                            else SettingsRepository.CatchupTimezone.MANUAL,
                        )
                    }
                },
            )
            if (tz == SettingsRepository.CatchupTimezone.MANUAL) {
                OffsetStepper(
                    minutes = offset,
                    range = vm.settings.catchupOffsetRangeMinutes,
                    onChange = { vm.edit { setCatchupOffsetMinutes(it) } },
                )
            }
            listOf(
                SettingsRepository.CatchupPlayer.ASK to R.string.settings_catchup_player_ask,
                SettingsRepository.CatchupPlayer.INTERNAL to R.string.settings_catchup_player_internal,
                SettingsRepository.CatchupPlayer.EXTERNAL to R.string.settings_catchup_player_external,
            ).forEach { (mode, label) ->
                SettingRow(
                    title = stringResource(label),
                    checked = player == mode,
                    onCheckedChange = { vm.edit { setCatchupPlayer(mode) } },
                )
            }
        }
    }
}

/** A whole hour at a time, the way the TV app's dialog steps it — providers publish hour offsets. */
@Composable
private fun OffsetStepper(minutes: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = { onChange((minutes - 60).coerceIn(range)) },
            enabled = minutes > range.first,
        ) {
            Text(stringResource(R.string.settings_decrease))
        }
        Text(text = utcOffsetLabel(minutes), style = MaterialTheme.typography.titleMedium)
        TextButton(
            onClick = { onChange((minutes + 60).coerceIn(range)) },
            enabled = minutes < range.last,
        ) {
            Text(stringResource(R.string.settings_increase))
        }
    }
}

/** "UTC+02:00" — a clock offset, not a translated phrase. */
internal fun utcOffsetLabel(minutes: Int): String {
    if (minutes == 0) return "UTC"
    val sign = if (minutes < 0) "-" else "+"
    val abs = kotlin.math.abs(minutes)
    return "UTC$sign%02d:%02d".format(java.util.Locale.ROOT, abs / 60, abs % 60)
}
