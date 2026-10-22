package tv.own.owntv.mobile.ui.screens.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.core.backup.BackupManager
import tv.own.owntv.core.sync.local.LocalAutoSyncTiming
import tv.own.owntv.core.sync.local.shortCodes
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.sheetListHeight
import tv.own.owntv.mobile.ui.stage.StageGroupLabel
import tv.own.owntv.mobile.ui.stage.StageRadio
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The auto-sync settings: what travels, to whom, and when it travels by itself.
 *
 * The shape is the one the television app uses — lists of questions under one heading, and a Save
 * button that is the only thing that makes anything real. Nothing here changes the saved settings as
 * it is touched: the sheet edits a draft, and only Save writes it down. A phone that saved as it
 * ticked would leave a half-finished schedule behind every time the user swiped the sheet away.
 *
 * The questions are the ones the sync and backup sheets ask — which parts of the data, whose
 * profiles, which devices, and (a thing a backup does not have) how often — so the rows say what the
 * rest of the app already says for them.
 *
 * The profile and device lists are only drawn when there is something to choose: a phone with one
 * viewer has already answered the profile question, and a row that can only be ticked again is
 * noise. A phone with no pairings still gets the heading and a line saying so, because "no devices
 * yet" is an answer the user needs to see rather than an empty space to wonder about.
 */
@Composable
fun LocalAutoSyncSheet(vm: LocalAutoSyncViewModel, onDismiss: () -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    // The draft. Null means "not touched yet", so the sheet shows what is saved until the user
    // changes something — the settings arrive from disk a moment after the sheet opens.
    var timing by remember { mutableStateOf<LocalAutoSyncTiming?>(null) }
    var sections by remember { mutableStateOf<Set<BackupManager.Section>?>(null) }
    var profileDraft by remember { mutableStateOf<Set<Long>?>(null) }
    var deviceDraft by remember { mutableStateOf<Set<String>?>(null) }
    // An empty set in the config means "everything", so a device that was never asked shows every
    // row ticked. Ids that no longer exist cannot be shown, and drop out of the intersection.
    val allProfiles = profiles.map { it.id }.toSet()
    val allDevices = devices.map { it.id }.toSet()
    val chosenTiming = timing ?: config.timing
    val chosenSections = sections ?: config.sections
    // With one profile there is no row to tick, so the hidden list means "everything" — including
    // when the saved answer names a profile that has since been deleted, which would otherwise
    // leave Save disabled with no row to press.
    val chosenProfiles = profileDraft
        ?: if (profiles.size <= 1) {
            allProfiles
        } else {
            config.profileIds.takeIf { it.isNotEmpty() }?.intersect(allProfiles) ?: allProfiles
        }
    val chosenDevices = deviceDraft
        ?: config.deviceIds.takeIf { it.isNotEmpty() }?.intersect(allDevices)
        ?: allDevices
    val deviceCodes = remember(devices) { shortCodes(devices) }
    val canSave = chosenSections.isNotEmpty() &&
        (allProfiles.isEmpty() || chosenProfiles.isNotEmpty()) &&
        (allDevices.isEmpty() || chosenDevices.isNotEmpty())

    val title = stringResource(R.string.local_auto_sync_settings_title)
    MobileBottomSheet(onDismissRequest = onDismiss, title = title) {
        StageSheetHeader(
            title = title,
            subtitle = stringResource(R.string.local_auto_sync_description),
        )
        Column(
            modifier = Modifier
                .heightIn(max = sheetListHeight())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MobileDimens.GapTiny),
        ) {
            StageGroupLabel(stringResource(R.string.local_auto_sync_what_to_sync))
            // The same everything-row the sync sheet has, because it is the same list: seven rows is
            // a long enough list that "all of it" deserves one tap of its own.
            SyncCheckRow(
                label = stringResource(R.string.local_sync_everything),
                description = null,
                checked = chosenSections.size == BackupManager.Section.entries.size,
                onToggle = { on -> sections = if (on) BackupManager.Section.entries.toSet() else emptySet() },
            )
            BackupManager.Section.entries.forEach { section ->
                SyncCheckRow(
                    label = stringResource(section.labelRes()),
                    description = stringResource(section.descriptionRes()),
                    checked = section in chosenSections,
                    onToggle = { on ->
                        sections = if (on) chosenSections + section else chosenSections - section
                    },
                )
            }
            if (profiles.size > 1) {
                StageGroupLabel(stringResource(R.string.local_auto_sync_what_profiles))
                profiles.forEach { profile ->
                    SyncCheckRow(
                        label = profile.name,
                        description = null,
                        checked = profile.id in chosenProfiles,
                        onToggle = { on ->
                            profileDraft = if (on) chosenProfiles + profile.id else chosenProfiles - profile.id
                        },
                    )
                }
            }
            StageGroupLabel(stringResource(R.string.local_auto_sync_what_devices))
            if (devices.isEmpty()) {
                SyncNote(stringResource(R.string.local_auto_sync_no_devices))
            }
            devices.forEach { device ->
                // Two phones of the same model share a name; the code is what tells them apart, and
                // the paired-device rows below the sheet use it for the same reason.
                val code = deviceCodes[device.id]
                SyncCheckRow(
                    label = if (code == null) {
                        device.name
                    } else {
                        stringResource(R.string.local_sync_device_with_code, device.name, code)
                    },
                    description = device.address,
                    checked = device.id in chosenDevices,
                    onToggle = { on ->
                        deviceDraft = if (on) chosenDevices + device.id else chosenDevices - device.id
                    },
                )
            }
            StageGroupLabel(stringResource(R.string.local_auto_sync_when))
            // "Never" is a timing like the others rather than a separate switch: one list, one
            // answer, and no way for the two to disagree.
            LocalAutoSyncTiming.entries.forEach { option ->
                StageSettingRow(
                    title = stringResource(option.labelRes()),
                    minHeight = 48.dp,
                    onClick = { timing = option },
                ) { StageRadio(selected = option == chosenTiming) }
            }
            // What the last run did, when there was one. A setting that says "daily" and a phone
            // that has not run in a week are both true at once, and only one of them is on screen
            // here — so the other one has to be.
            if (config.lastRunAt > 0L) {
                SyncNote(stringResource(R.string.local_auto_sync_last_run, lastRunText(config.lastRunAt)))
            }
            SyncNote(stringResource(R.string.local_auto_sync_note))
        }
        SyncButtons(
            confirm = stringResource(R.string.local_auto_sync_save),
            // Nothing ticked means nothing to sync. The button says so rather than saving a
            // schedule that would go and fetch a whole backup to do nothing with it.
            confirmEnabled = canSave,
            onConfirm = {
                vm.save(
                    timing = chosenTiming,
                    sections = chosenSections,
                    // Everything ticked is saved as "all", not as a list of today's ids: a viewer
                    // added next week belongs in a schedule that says "all of them".
                    profileIds = if (chosenProfiles.size == allProfiles.size) emptySet() else chosenProfiles,
                    deviceIds = if (chosenDevices.size == allDevices.size) emptySet() else chosenDevices,
                )
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

/** The word on the "When" rows, and the one on the Auto-sync card. */
internal fun LocalAutoSyncTiming.labelRes(): Int = when (this) {
    LocalAutoSyncTiming.OFF -> R.string.local_auto_sync_timing_off
    LocalAutoSyncTiming.STARTUP -> R.string.local_auto_sync_timing_immediately
    LocalAutoSyncTiming.HOURS_6 -> R.string.local_auto_sync_timing_6h
    LocalAutoSyncTiming.HOURS_24 -> R.string.local_auto_sync_timing_daily
    LocalAutoSyncTiming.DAYS_7 -> R.string.local_auto_sync_timing_weekly
}

/** "3 minutes ago", the same phrasing the paired-device rows use for their last sync. */
private fun lastRunText(at: Long): String = DateUtils.getRelativeTimeSpanString(
    at,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
).toString()
