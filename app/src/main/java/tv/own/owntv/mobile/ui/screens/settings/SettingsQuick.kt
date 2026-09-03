package tv.own.owntv.mobile.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow

/**
 * A switch that can be lifted to the top of the settings root.
 *
 * The pinned list is core's, shared with the TV app, and core keeps keys it does not recognise — so a
 * phone pinning "Background playback" does not disturb a television that has no such row, and the six
 * the TV app pins by default arrive here with the two remote-control ones simply not drawn.
 */
class QuickToggle(
    val key: String,
    @param:StringRes val titleRes: Int,
    val flow: (SettingsRepository) -> Flow<Boolean>,
    val set: suspend SettingsRepository.(Boolean) -> Unit,
)

/** Every switch this app lets the user pin, by the key it is stored under. */
val QUICK_TOGGLES: List<QuickToggle> = listOf(
    QuickToggle("quick_hdr", R.string.settings_quick_hdr, { it.hdrEnabled }, { setHdrEnabled(it) }),
    QuickToggle(
        "quick_autoplay",
        R.string.settings_quick_autoplay,
        { it.autoPlayNext },
        { setAutoPlayNext(it) },
    ),
    QuickToggle(
        "background_playback",
        R.string.settings_background_playback,
        { it.backgroundPlayback },
        { setBackgroundPlayback(it) },
    ),
    QuickToggle("pip_enabled", R.string.settings_pip, { it.pipEnabled }, { setPipEnabled(it) }),
    QuickToggle("data_saver", R.string.settings_data_saver, { it.dataSaver }, { setDataSaver(it) }),
    QuickToggle(
        "downloads_wifi_only",
        R.string.settings_downloads_wifi_only,
        { it.downloadsWifiOnly },
        { setDownloadsWifiOnly(it) },
    ),
    QuickToggle(
        "detailed_diagnostics",
        R.string.settings_diagnostics,
        { it.detailedDiagnostics },
        { setDetailedDiagnostics(it) },
    ),
)

/**
 * A switch row that offers "Pin to Quick" on a long press.
 *
 * Every pinnable setting is drawn by this one composable, on its own page and again in Quick, so the
 * two can never show different states — there is only one row.
 */
@Composable
fun QuickSwitchRow(
    vm: SettingsViewModel,
    toggle: QuickToggle,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val checked = toggle.flow(vm.settings).pref(false)
    val pinned = vm.settings.quickPinnedKeys.pref(emptyList())
    var menu by remember { mutableStateOf(false) }

    MobileListRow(
        title = stringResource(toggle.titleRes),
        subtitle = subtitle,
        modifier = modifier,
        onClick = { vm.edit { toggle.set(this, !checked) } },
        onLongClick = { menu = true },
        trailing = {
            Switch(checked = checked, onCheckedChange = { vm.edit { toggle.set(this, it) } })
        },
    )

    if (menu) {
        val isPinned = toggle.key in pinned
        MobileBottomSheet(onDismissRequest = { menu = false }, title = stringResource(toggle.titleRes)) {
            MobileListRow(
                title = stringResource(
                    if (isPinned) R.string.settings_row_menu_unpin else R.string.settings_row_menu_pin,
                ),
                onClick = {
                    val next =
                        if (isPinned) pinned - toggle.key
                        else pinned + toggle.key
                    vm.edit { setQuickPinnedKeys(next) }
                    menu = false
                },
            )
        }
    }
}
