package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.SettingRow

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
        item(key = "pip") {
            QuickSwitchRow(
                vm = vm,
                toggle = quickToggle("pip_enabled"),
                subtitle = stringResource(R.string.settings_pip_description),
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
}
