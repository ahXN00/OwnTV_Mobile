package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.epg.EpgSource
import tv.own.owntv.core.model.SourceType
import tv.own.owntv.core.settings.EpgAutoRefresh
import tv.own.owntv.core.settings.PlaylistAutoRefresh
import tv.own.owntv.core.settings.PlaylistRefresh
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * Where the content comes from: the playlists, the guide feeds that fill the EPG, and the two clock
 * settings that decide whether a programme's times land where the provider meant them to.
 *
 * A playlist's own form is the setup flow's — the same one a first run uses — so this page opens it
 * rather than owning a second copy of it.
 */
@Composable
fun SettingsSourcesPage(
    onAddSource: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    val sources by vm.sources.collectAsStateWithLifecycle()
    val epgSources by vm.epgSources.collectAsStateWithLifecycle()
    val playlistRefresh = vm.settings.playlistAutoRefresh.pref(emptyMap())
    val epgRefresh = vm.settings.epgAutoRefresh.pref(emptyMap())
    val useLogos = vm.settings.epgUseLogos.pref(emptySet())

    var menuSource by remember { mutableStateOf<SourceEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<SourceEntity?>(null) }
    var refreshFor by remember { mutableStateOf<SourceEntity?>(null) }
    var menuEpg by remember { mutableStateOf<EpgSource?>(null) }
    var epgRefreshFor by remember { mutableStateOf<EpgSource?>(null) }
    var addEpg by remember { mutableStateOf(false) }
    var epgOffsetSheet by remember { mutableStateOf(false) }
    var catchupSheet by remember { mutableStateOf(false) }

    SettingsPage(modifier) {
        settingsSection(R.string.settings_playlists)
        if (sources.isEmpty()) {
            settingsNote(R.string.settings_sources_empty)
        }
        items(sources, key = { it.id }) { source ->
            val state by vm.syncState(source.id).collectAsStateWithLifecycle(null)
            MobileListRow(
                title = source.name,
                subtitle = if (state?.isActive == true) {
                    stringResource(R.string.settings_sources_importing)
                } else {
                    stringResource(source.type.labelRes()) + source.url
                },
                onClick = { menuSource = source },
                onLongClick = { menuSource = source },
            )
        }
        item(key = "add-source") {
            MobileListRow(
                title = stringResource(R.string.settings_sources_add),
                leading = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onAddSource,
            )
        }

        settingsSection(R.string.settings_epg_sources)
        settingsNote(R.string.settings_epg_sources_description)
        if (epgSources.isEmpty()) {
            settingsNote(R.string.settings_epg_sources_empty)
        }
        items(epgSources, key = { it.id }) { source ->
            MobileListRow(
                title = source.name,
                subtitle = source.lastError
                    ?: if (source.lastSyncAt == null) {
                        stringResource(R.string.settings_epg_sources_not_synced)
                    } else {
                        source.url
                    },
                onClick = { menuEpg = source },
                onLongClick = { menuEpg = source },
            )
        }
        item(key = "add-epg") {
            MobileListRow(
                title = stringResource(R.string.settings_epg_sources_add),
                leading = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { addEpg = true },
            )
        }

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

    menuSource?.let { source ->
        MobileBottomSheet(onDismissRequest = { menuSource = null }, title = source.name) {
            MobileListRow(
                title = stringResource(R.string.settings_sources_resync_now_full),
                subtitle = stringResource(R.string.settings_sources_resync_description),
                onClick = { vm.resync(source); menuSource = null },
            )
            MobileListRow(
                title = stringResource(R.string.settings_sources_resync_remove_full),
                onClick = { vm.resync(source, removeMissing = true); menuSource = null },
            )
            MobileListRow(
                title = stringResource(R.string.settings_epg_sources_auto_refresh_title),
                subtitle = stringResource(R.string.settings_epg_sources_auto_refresh_description),
                onClick = { refreshFor = source; menuSource = null },
            )
            MobileListRow(
                title = stringResource(R.string.settings_sources_delete),
                onClick = { confirmDelete = source; menuSource = null },
            )
        }
    }

    refreshFor?.let { source ->
        val current = playlistRefresh[source.id]?.mode ?: PlaylistAutoRefresh.OFF
        SettingsChoiceSheet(
            title = stringResource(R.string.settings_epg_sources_auto_refresh_title),
            choices = listOf(
                SettingsChoice(PlaylistAutoRefresh.OFF, stringResource(R.string.settings_sources_refresh_off)),
                SettingsChoice(
                    PlaylistAutoRefresh.STARTUP,
                    stringResource(R.string.settings_sources_refresh_startup),
                ),
                SettingsChoice(PlaylistAutoRefresh.HOURS_6, stringResource(R.string.settings_sources_refresh_6h)),
                SettingsChoice(PlaylistAutoRefresh.HOURS_12, stringResource(R.string.settings_sources_refresh_12h)),
            ),
            selected = current,
            onSelect = { mode ->
                vm.edit {
                    setPlaylistAutoRefresh(
                        source.id,
                        (playlistRefresh[source.id] ?: PlaylistRefresh()).copy(mode = mode),
                    )
                }
            },
            onDismiss = { refreshFor = null },
        )
    }

    confirmDelete?.let { source ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.settings_sources_delete_title) + source.name) },
            text = { Text(stringResource(R.string.settings_sources_delete_message)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteSource(source); confirmDelete = null }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    menuEpg?.let { source ->
        val logosOn = source.id in useLogos
        MobileBottomSheet(onDismissRequest = { menuEpg = null }, title = source.name) {
            MobileListRow(
                title = stringResource(R.string.settings_sync_now),
                onClick = { vm.syncEpg(source); menuEpg = null },
            )
            SettingRow(
                title = stringResource(R.string.settings_epg_sources_use_logos),
                subtitle = stringResource(R.string.settings_epg_sources_logos_description),
                checked = logosOn,
                onCheckedChange = { vm.edit { setEpgUseLogos(source.id, it) } },
            )
            MobileListRow(
                title = stringResource(R.string.settings_epg_sources_auto_refresh_title),
                onClick = { epgRefreshFor = source; menuEpg = null },
            )
            MobileListRow(
                title = stringResource(R.string.settings_sources_delete),
                onClick = { vm.removeEpg(source); menuEpg = null },
            )
        }
    }

    epgRefreshFor?.let { source ->
        SettingsChoiceSheet(
            title = stringResource(R.string.settings_epg_sources_auto_refresh_title),
            choices = listOf(
                SettingsChoice(EpgAutoRefresh.OFF, stringResource(R.string.settings_sources_refresh_off)),
                SettingsChoice(EpgAutoRefresh.STARTUP, stringResource(R.string.settings_sources_refresh_startup)),
                SettingsChoice(EpgAutoRefresh.HOURS_1, stringResource(R.string.settings_epg_refresh_1h)),
                SettingsChoice(EpgAutoRefresh.HOURS_3, stringResource(R.string.settings_epg_refresh_3h)),
                SettingsChoice(EpgAutoRefresh.HOURS_6, stringResource(R.string.settings_epg_refresh_6h)),
                SettingsChoice(EpgAutoRefresh.HOURS_12, stringResource(R.string.settings_epg_refresh_12h)),
                SettingsChoice(EpgAutoRefresh.HOURS_24, stringResource(R.string.settings_epg_refresh_24h)),
                SettingsChoice(EpgAutoRefresh.HOURS_48, stringResource(R.string.settings_epg_refresh_48h)),
            ),
            selected = epgRefresh[source.id] ?: EpgAutoRefresh.OFF,
            onSelect = { mode -> vm.edit { setEpgAutoRefresh(source.id, mode) } },
            onDismiss = { epgRefreshFor = null },
        )
    }

    if (addEpg) {
        AddEpgDialog(
            onDismiss = { addEpg = false },
            onAdd = { name, url, agent -> vm.addEpg(name, url, agent); addEpg = false },
        )
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

@Composable
private fun AddEpgDialog(onDismiss: () -> Unit, onAdd: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var agent by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_epg_sources_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_epg_sources_name)) },
                    placeholder = { Text(stringResource(R.string.settings_epg_sources_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_epg_sources_url)) },
                    placeholder = { Text(stringResource(R.string.settings_epg_sources_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = agent,
                    onValueChange = { agent = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_epg_sources_user_agent)) },
                    placeholder = { Text(stringResource(R.string.settings_epg_sources_user_agent_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(name.trim(), url.trim(), agent.trim()) },
                enabled = name.isNotBlank() && url.isNotBlank(),
            ) {
                Text(stringResource(R.string.settings_epg_sources_add_sync))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun SourceType.labelRes(): Int = when (this) {
    SourceType.M3U -> R.string.settings_sources_type_m3u
    SourceType.XTREAM -> R.string.settings_sources_type_xtream
    SourceType.STALKER -> R.string.settings_sources_type_stalker
    SourceType.LOCAL_BACKUP -> R.string.settings_sources_backup
}

/** "UTC+02:00" — a clock offset, not a translated phrase. */
private fun utcOffsetLabel(minutes: Int): String {
    if (minutes == 0) return "UTC"
    val sign = if (minutes < 0) "-" else "+"
    val abs = kotlin.math.abs(minutes)
    return "UTC$sign%02d:%02d".format(java.util.Locale.ROOT, abs / 60, abs % 60)
}
