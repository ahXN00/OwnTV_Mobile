package tv.own.owntv.mobile.ui.screens.downloads

import tv.own.owntv.mobile.ui.components.MobileIcons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.database.entity.DownloadEntity
import tv.own.owntv.core.model.DownloadStatus
import tv.own.owntv.core.storage.StorageAccess
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.FilterChipRow
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.theme.MobileDimens
import java.text.NumberFormat

/**
 * Films and episodes saved for watching with the network off.
 *
 * Three chips instead of the television's four headings — a phone list is scrolled, not scanned, and
 * a user opening this screen is looking for one of three things: what is coming down now, what is
 * ready, or what went wrong. Anything still queued counts as coming down.
 */
@Composable
fun DownloadsScreen(
    onPlayerOpened: () -> Unit,
    modifier: Modifier = Modifier,
    vm: DownloadsViewModel = koinViewModel(),
) {
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val speed by vm.speedMbps.collectAsStateWithLifecycle()
    val root by vm.downloadRoot.collectAsStateWithLifecycle()
    val playing by vm.playing.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(DownloadTab.ACTIVE) }
    var menuFor by remember { mutableStateOf<DownloadEntity?>(null) }
    var volumePicker by remember { mutableStateOf(false) }
    var savingCopyOf by remember { mutableStateOf<DownloadEntity?>(null) }

    val saveCopy = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("video/*"),
    ) { uri ->
        val download = savingCopyOf
        savingCopyOf = null
        if (uri != null && download != null) vm.saveCopy(download, uri)
    }

    LaunchedEffect(playing) {
        if (playing) {
            vm.playerOpened()
            onPlayerOpened()
        }
    }

    val shown = downloads.filter { tab.holds(it.status) }

    Column(modifier.fillMaxSize()) {
        StorageHeader(
            storage = storage,
            speedMbps = speed,
            onPickVolume = { volumePicker = true },
        )
        FilterChipRow(
            labels = DownloadTab.entries.map { stringResource(it.labelRes) },
            selectedIndex = DownloadTab.entries.indexOf(tab),
            onSelect = { tab = DownloadTab.entries[it] },
        )

        if (shown.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(MobileDimens.GapLarge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.content_downloads_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.id }) { download ->
                    DownloadRow(
                        download = download,
                        onClick = {
                            if (download.status == DownloadStatus.COMPLETED) vm.play(download)
                            else menuFor = download
                        },
                        onLongClick = { menuFor = download },
                    )
                }
            }
        }
    }

    menuFor?.let { download ->
        DownloadMenu(
            download = download,
            vm = vm,
            onSaveCopy = {
                savingCopyOf = download
                saveCopy.launch(download.filePath?.substringAfterLast('/') ?: download.title)
            },
            onDismiss = { menuFor = null },
        )
    }

    if (volumePicker) {
        VolumePicker(
            volumes = vm.volumes,
            current = root,
            onPick = { vm.setDownloadRoot(it); volumePicker = false },
            onDismiss = { volumePicker = false },
        )
    }
}

/** Queued counts as active: from the user's side it is a download that has not arrived yet. */
private enum class DownloadTab(val labelRes: Int) {
    ACTIVE(R.string.content_downloads_active),
    COMPLETED(R.string.content_downloads_completed_group),
    FAILED(R.string.content_downloads_failed_group),
    ;

    fun holds(status: DownloadStatus): Boolean = when (this) {
        ACTIVE -> status == DownloadStatus.RUNNING || status == DownloadStatus.PAUSED ||
            status == DownloadStatus.QUEUED
        COMPLETED -> status == DownloadStatus.COMPLETED
        FAILED -> status == DownloadStatus.FAILED
    }
}

@Composable
private fun StorageHeader(
    storage: tv.own.owntv.core.download.DownloadStorageInfo?,
    speedMbps: Double,
    onPickVolume: () -> Unit,
) {
    val unknown = stringResource(R.string.content_downloads_unknown_size)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = MobileDimens.ScreenPaddingH,
                end = MobileDimens.GapSmall,
                top = MobileDimens.GapSmall,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.content_downloads_storage),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = storage?.let {
                    stringResource(
                        R.string.content_downloads_storage_free,
                        gigabytes(it.freeBytes, unknown),
                        gigabytes(it.totalBytes, unknown),
                    )
                } ?: unknown,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MobileDimens.GapSmall),
            )
            if (speedMbps > 0.0) {
                Text(
                    text = stringResource(R.string.player_stream_mbps, decimal(speedMbps)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPickVolume) {
                Icon(
                    imageVector = MobileIcons.Folder,
                    contentDescription = stringResource(R.string.settings_download_folder),
                    // Without a tint this inherits a content colour meant for a filled button and
                    // comes out black on the dark card.
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        storage?.let {
            LinearProgressIndicator(
                progress = { it.usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = MobileDimens.GapSmall, bottom = MobileDimens.GapSmall),
            )
        }
    }
}

@Composable
private fun DownloadRow(download: DownloadEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    val unknown = stringResource(R.string.content_downloads_unknown_size)
    val fraction = if (download.totalBytes > 0) {
        (download.downloadedBytes.toFloat() / download.totalBytes).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column {
        MobileListRow(
            title = download.title,
            subtitle = statusLine(download, unknown),
            leading = {
                Icon(
                    imageVector = MobileIcons.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailing = {
                if (download.status == DownloadStatus.COMPLETED) {
                    Icon(
                        imageVector = MobileIcons.PlayArrow,
                        contentDescription = stringResource(R.string.content_downloads_play),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            onClick = onClick,
            onLongClick = onLongClick,
        )
        if (download.status == DownloadStatus.RUNNING || download.status == DownloadStatus.PAUSED) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MobileDimens.ScreenPaddingH),
            )
        }
    }
}

/** The second line of a row: where it got to, or why it stopped. */
@Composable
private fun statusLine(download: DownloadEntity, unknown: String): String = when (download.status) {
    DownloadStatus.COMPLETED -> stringResource(
        R.string.content_downloads_completed,
        megabytes(download.totalBytes, unknown),
    )
    DownloadStatus.FAILED -> stringResource(R.string.content_downloads_failure_message)
    DownloadStatus.QUEUED -> stringResource(R.string.content_downloads_queued)
    else -> if (download.totalBytes > 0) {
        stringResource(
            R.string.content_downloads_progress,
            ((download.downloadedBytes.toFloat() / download.totalBytes).coerceIn(0f, 1f) * 100).toInt(),
            megabytes(download.downloadedBytes, unknown),
            megabytes(download.totalBytes, unknown),
        )
    } else {
        stringResource(R.string.content_downloads_progress_unknown, megabytes(download.downloadedBytes, unknown))
    }
}

@Composable
private fun DownloadMenu(
    download: DownloadEntity,
    vm: DownloadsViewModel,
    onSaveCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    MobileBottomSheet(onDismissRequest = onDismiss, title = download.title) {
        when (download.status) {
            DownloadStatus.COMPLETED -> {
                MenuRow(R.string.content_downloads_play, MobileIcons.PlayArrow) {
                    vm.play(download); onDismiss()
                }
                MenuRow(R.string.settings_export, MobileIcons.Save) { onSaveCopy(); onDismiss() }
            }
            DownloadStatus.FAILED -> MenuRow(R.string.common_retry, MobileIcons.Refresh) {
                vm.retry(download); onDismiss()
            }
            DownloadStatus.PAUSED -> MenuRow(R.string.common_resume, MobileIcons.PlayArrow) {
                vm.resume(download); onDismiss()
            }
            else -> MenuRow(R.string.content_downloads_pause, MobileIcons.Pause) {
                vm.pause(download); onDismiss()
            }
        }
        HorizontalDivider()
        MenuRow(R.string.common_delete, MobileIcons.Delete, destructive = true) {
            vm.delete(download); onDismiss()
        }
    }
}

@Composable
private fun VolumePicker(
    volumes: List<StorageAccess.StorageRoot>,
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_download_folder_title),
    ) {
        volumes.forEach { volume ->
            val path = volume.file.absolutePath
            MobileListRow(
                title = stringResource(
                    if (volume.kind == StorageAccess.RootKind.REMOVABLE) R.string.content_storage_removable
                    else R.string.content_storage_internal,
                ),
                subtitle = volume.volumeName ?: path,
                leading = {
                    Icon(
                        imageVector = if (volume.kind == StorageAccess.RootKind.REMOVABLE) {
                            MobileIcons.SdStorage
                        } else {
                            MobileIcons.Folder
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailing = {
                    // An unset folder means the default, which is the first volume in the list.
                    val chosen = if (current.isBlank()) volume == volumes.firstOrNull() else current == path
                    if (chosen) {
                        Icon(
                            imageVector = MobileIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                onClick = { onPick(path) },
            )
        }
    }
}

@Composable
private fun MenuRow(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    MobileListRow(
        title = stringResource(labelRes),
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
    )
}

private fun gigabytes(bytes: Long, unknown: String): String =
    if (bytes <= 0) unknown else decimal(bytes / 1_073_741_824.0)

private fun megabytes(bytes: Long, unknown: String): String =
    if (bytes <= 0) unknown else decimal(bytes / 1_048_576.0)

private fun decimal(value: Double): String = NumberFormat.getNumberInstance().apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}.format(value)
