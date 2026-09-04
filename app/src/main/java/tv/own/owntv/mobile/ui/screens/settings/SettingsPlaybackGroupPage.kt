package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.SettingRow

/** The two "one of these" questions in the Mobile section. */
private enum class MobilePlaybackSheet { MINI_STYLE, PIP_SIZE }

/**
 * Two rows into the player's own screens, then the settings a television has no use for.
 *
 * Everything the TV app has lives behind Video player and Subtitle appearance, keyed exactly as it is
 * there — external player included, which sits in the Video player's Engine section the way it does
 * on the television. What is below the "Mobile" heading has no television equivalent at all: a screen
 * that gets locked, a window that floats over another app, a data plan that gets billed, and a finger
 * that is less precise than a remote.
 */
@Composable
fun SettingsPlaybackPage(
    onOpenLeaf: (SettingsLeaf) -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    val s = vm.settings
    val gesture = s.gestureSensitivityPct.pref(100)
    val miniStyle = s.miniPlayerStyle.pref(SettingsRepository.MiniPlayerStyle.FLOATING)
    val pipSize = s.pipSize.pref(SettingsRepository.PipSize.MEDIUM)

    var sheet by remember { mutableStateOf<MobilePlaybackSheet?>(null) }

    SettingsPage(modifier) {
        settingsLeafRows(SettingsGroup.PLAYBACK, onOpenLeaf)

        settingsSection(R.string.settings_playback_mobile)
        item(key = "background-playback") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("background_playback"),
                subtitle = stringResource(R.string.settings_background_playback_description),
            )
        }
        item(key = "mini-style") {
            SettingRow(
                title = stringResource(R.string.settings_mini_player_style),
                subtitle = stringResource(R.string.settings_mini_player_style_description),
                value = stringResource(miniStyle.labelRes()),
                onClick = { sheet = MobilePlaybackSheet.MINI_STYLE },
            )
        }
        item(key = "pip") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("pip_enabled"),
                subtitle = stringResource(R.string.settings_pip_description),
            )
        }
        item(key = "pip-on-back") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("pip_on_back"),
                subtitle = stringResource(R.string.settings_pip_on_back_description),
            )
        }
        item(key = "pip-size") {
            SettingRow(
                title = stringResource(R.string.settings_pip_size),
                subtitle = stringResource(R.string.settings_pip_size_description),
                value = stringResource(pipSize.labelRes()),
                onClick = { sheet = MobilePlaybackSheet.PIP_SIZE },
            )
        }
        item(key = "pip-snap") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("pip_snap"),
                subtitle = stringResource(R.string.settings_pip_snap_description),
            )
        }
        item(key = "audio-screen-off") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("audio_on_screen_off"),
                subtitle = stringResource(R.string.settings_audio_on_screen_off_description),
            )
        }
        item(key = "audio-mobile-data") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("audio_on_mobile_data"),
                subtitle = stringResource(R.string.settings_audio_on_mobile_data_description),
            )
        }
        item(key = "audio-per-channel") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("audio_per_channel"),
                subtitle = stringResource(R.string.settings_audio_per_channel_description),
            )
        }
        item(key = "data-saver") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("data_saver"),
                subtitle = stringResource(R.string.settings_data_saver_description),
            )
        }
        item(key = "gesture") {
            SettingsSlider(
                title = stringResource(R.string.settings_gesture_sensitivity),
                subtitle = stringResource(R.string.settings_gesture_sensitivity_description),
                value = gesture,
                range = 50..200,
                onValueChange = { pct -> vm.edit { setGestureSensitivityPct(pct) } },
            )
        }
    }

    when (sheet) {
        MobilePlaybackSheet.MINI_STYLE -> SettingsChoiceSheet(
            title = stringResource(R.string.settings_mini_player_style),
            choices = SettingsRepository.MiniPlayerStyle.entries.map {
                SettingsChoice(it, stringResource(it.labelRes()))
            },
            selected = miniStyle,
            onSelect = { style -> vm.edit { setMiniPlayerStyle(style) } },
            onDismiss = { sheet = null },
        )
        MobilePlaybackSheet.PIP_SIZE -> SettingsChoiceSheet(
            title = stringResource(R.string.settings_pip_size),
            choices = SettingsRepository.PipSize.entries.map {
                SettingsChoice(it, stringResource(it.labelRes()))
            },
            selected = pipSize,
            onSelect = { size -> vm.edit { setPipSize(size) } },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

private fun SettingsRepository.MiniPlayerStyle.labelRes(): Int = when (this) {
    SettingsRepository.MiniPlayerStyle.FLOATING -> R.string.settings_mini_player_style_floating
    SettingsRepository.MiniPlayerStyle.DOCKED -> R.string.settings_mini_player_style_docked
    SettingsRepository.MiniPlayerStyle.OFF -> R.string.common_off
}

private fun SettingsRepository.PipSize.labelRes(): Int = when (this) {
    SettingsRepository.PipSize.SMALL -> R.string.settings_pip_size_small
    SettingsRepository.PipSize.MEDIUM -> R.string.settings_pip_size_medium
    SettingsRepository.PipSize.LARGE -> R.string.settings_pip_size_large
}
