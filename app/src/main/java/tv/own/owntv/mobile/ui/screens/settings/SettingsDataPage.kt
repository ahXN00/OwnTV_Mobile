package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.storage.StorageAccess
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.theme.MobileDimens
import java.io.File
import tv.own.owntv.mobile.ui.theme.glassDialogWindow

/** The history scopes the clear-history sheet offers, "All history" first. */
private val HISTORY_SCOPES: List<MediaType?> =
    listOf(null, MediaType.LIVE, MediaType.MOVIE, MediaType.SERIES)

/**
 * Where downloads are written, when they are allowed to run, and how to throw watch history away.
 *
 * The folder list is only the roots the app can write to without a permission — a phone has a
 * document picker for everywhere else, and asking for all-files access to save a film is not a
 * trade a user should have to make.
 */
@Composable
fun SettingsDataPage(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    val root = vm.settings.downloadRoot.pref("")
    val wifiOnly = vm.settings.downloadsWifiOnly.pref(false)

    var volumes by remember { mutableStateOf<List<StorageAccess.StorageRoot>>(emptyList()) }
    LaunchedEffect(Unit) { volumes = vm.downloadVolumes() }

    var folderSheet by remember { mutableStateOf(false) }
    var historySheet by remember { mutableStateOf(false) }
    // An index, not the scope itself: null is a real scope here — it means "all history".
    var confirming by remember { mutableIntStateOf(-1) }

    SettingsPage(modifier) {
        settingsSection(R.string.settings_group_data) {
            SettingRow(
                title = stringResource(R.string.settings_download_folder),
                subtitle = root.ifBlank { null }?.let { File(it).name },
                value = if (root.isBlank()) stringResource(R.string.settings_app_storage) else null,
                onClick = { folderSheet = true },
            )
            SettingRow(
                title = stringResource(R.string.settings_downloads_wifi_only),
                subtitle = stringResource(R.string.settings_downloads_wifi_only_description),
                checked = wifiOnly,
                onCheckedChange = { on -> vm.edit { setDownloadsWifiOnly(on) } },
            )
            SettingRow(
                title = stringResource(R.string.settings_clear_history),
                subtitle = stringResource(R.string.settings_clear_history_description),
                onClick = { historySheet = true },
            )
        }
    }

    if (folderSheet) {
        SettingsChoiceSheet(
            title = stringResource(R.string.settings_download_folder_title),
            choices = volumes.map { volume ->
                SettingsChoice(
                    value = volume.file.absolutePath,
                    label = volume.volumeName ?: stringResource(volume.kind.labelRes()),
                    description = volume.file.absolutePath,
                )
            },
            selected = root,
            onSelect = { path -> vm.edit { setDownloadRoot(path) } },
            onDismiss = { folderSheet = false },
        )
    }

    if (historySheet) {
        // Not a SettingsChoiceSheet: every row here is an action, so none of them is "the current one".
        MobileBottomSheet(
            onDismissRequest = { historySheet = false },
            title = stringResource(R.string.settings_clear_history),
        ) {
            Text(
                text = stringResource(R.string.settings_choose_history),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
            )
            HISTORY_SCOPES.forEachIndexed { index, scope ->
                MobileListRow(
                    title = stringResource(scope.scopeLabelRes()),
                    onClick = { confirming = index; historySheet = false },
                )
            }
        }
    }

    if (confirming >= 0) {
        val scope = HISTORY_SCOPES[confirming]
        AlertDialog(
            modifier = Modifier.glassDialogWindow(),
            onDismissRequest = { confirming = -1 },
            title = {
                Text(
                    stringResource(
                        R.string.settings_clear_history_confirm,
                        stringResource(scope.scopeLabelRes()),
                    ),
                )
            },
            text = { Text(stringResource(R.string.settings_cannot_undo)) },
            confirmButton = {
                TextButton(
                    onClick = { vm.clearHistory(scope); confirming = -1 },
                ) { Text(stringResource(R.string.settings_yes_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = -1 }) { Text(stringResource(R.string.settings_no)) }
            },
        )
    }
}

private fun StorageAccess.RootKind.labelRes() = when (this) {
    StorageAccess.RootKind.INTERNAL -> R.string.content_storage_internal
    StorageAccess.RootKind.REMOVABLE -> R.string.content_storage_removable
    StorageAccess.RootKind.APP -> R.string.content_storage_app
}

private fun MediaType?.scopeLabelRes() = when (this) {
    MediaType.LIVE -> R.string.common_nav_live_tv
    MediaType.MOVIE -> R.string.common_nav_movies
    MediaType.SERIES -> R.string.common_nav_series
    else -> R.string.settings_all_history
}
