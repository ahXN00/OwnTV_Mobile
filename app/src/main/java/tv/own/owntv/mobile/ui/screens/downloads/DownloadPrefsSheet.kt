package tv.own.owntv.mobile.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.storage.StorageAccess
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.screens.settings.SettingsChoice
import tv.own.owntv.mobile.ui.screens.settings.SettingsChoiceSheet
import tv.own.owntv.mobile.ui.screens.settings.SettingsViewModel
import tv.own.owntv.mobile.ui.screens.settings.pref
import java.io.File

/**
 * Where downloads are written, and whether they wait for Wi-Fi.
 *
 * Both used to be rows in Settings → Data, which no longer exists — they belong on the screen you
 * are looking at when you care about them, which is why they hang off the gear in the Downloads
 * bar. The folder list is only the roots the app can write to without a permission: a phone has a
 * document picker for everywhere else, and asking for all-files access to save a film is not a
 * trade a user should have to make.
 */
@Composable
fun DownloadPrefsSheet(onDismiss: () -> Unit, vm: SettingsViewModel = koinViewModel()) {
    val root = vm.settings.downloadRoot.pref("")
    val wifiOnly = vm.settings.downloadsWifiOnly.pref(false)

    var volumes by remember { mutableStateOf<List<StorageAccess.StorageRoot>>(emptyList()) }
    LaunchedEffect(Unit) { volumes = vm.downloadVolumes() }

    var folderSheet by remember { mutableStateOf(false) }

    // The folder chooser is a sheet of its own, so this one steps aside while it is up rather than
    // stacking two sheets on top of each other.
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
        return
    }

    MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_download_folder_title),
    ) {
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
    }
}

private fun StorageAccess.RootKind.labelRes() = when (this) {
    StorageAccess.RootKind.INTERNAL -> R.string.content_storage_internal
    StorageAccess.RootKind.REMOVABLE -> R.string.content_storage_removable
    StorageAccess.RootKind.APP -> R.string.content_storage_app
}
