package tv.own.owntv.mobile.ui.screens.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.core.backup.BackupManager
import tv.own.owntv.core.sync.local.LocalAutoSyncTiming
import tv.own.owntv.core.sync.local.PairedDevice
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.components.sheetListHeight
import tv.own.owntv.mobile.ui.stage.StageGroupLabel
import tv.own.owntv.mobile.ui.stage.StageMenuItem
import tv.own.owntv.mobile.ui.stage.StageRadio
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * One paired device's auto-sync settings: what travels to it, and when it travels by itself.
 *
 * The first row is the switch, and it is different from every row under it. Turning it on does not
 * write a setting: it asks the other device, whose screen shows the question, and only its yes
 * starts the schedule. That is why the rest of the sheet is inert until the switch is on — there is
 * no schedule to scope yet, and a row that looked editable would be a promise the sheet cannot keep
 * until the other device agrees. Turning it off is local and immediate.
 *
 * The shape of the rest is the one the television app uses — lists of questions under one heading,
 * and a Save button that is the only thing that makes anything real. Nothing there changes the saved
 * settings as it is touched: the sheet edits a draft, and only Save writes it down. A phone that
 * saved as it ticked would leave a half-finished schedule behind every time the user swiped the
 * sheet away.
 *
 * The questions are the ones the sync and backup sheets ask — which parts of the data, whose
 * profiles, and (a thing a backup does not have) how often — so the rows say what the rest of the
 * app already says for them. The device is not one of the questions: [device] is the answer to it,
 * and the row that opened this sheet already named it.
 *
 * The profile list is only drawn when there is something to choose: a phone with one viewer has
 * already answered that question, and a row that can only be ticked again is noise.
 */
@Composable
fun LocalAutoSyncSheet(vm: LocalAutoSyncViewModel, device: PairedDevice, onDismiss: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val consents by vm.consents.collectAsStateWithLifecycle()
    val enableState by vm.enableState.collectAsStateWithLifecycle()
    // The draft. Null means "not touched yet", so the sheet shows what is saved until the user
    // changes something — the settings arrive from disk a moment after the sheet opens.
    var timing by remember(device.id) { mutableStateOf<LocalAutoSyncTiming?>(null) }
    var sections by remember(device.id) { mutableStateOf<Set<BackupManager.Section>?>(null) }
    var profileDraft by remember(device.id) { mutableStateOf<Set<Long>?>(null) }
    val config = settings[device.id]
    val asking = enableState is LocalAutoSyncViewModel.EnableState.Asking
    // Whether the rows under the switch can be touched at all.
    val editable = config.enabled && !asking
    // An empty set in the config means "everything", so a device that was never asked shows every
    // row ticked. Ids that no longer exist cannot be shown, and drop out of the intersection.
    val allProfiles = profiles.map { it.id }.toSet()
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
    val canSave = editable && chosenSections.isNotEmpty() &&
        (allProfiles.isEmpty() || chosenProfiles.isNotEmpty())

    // Closing the sheet closes the question too: an answer that arrives after the user is gone would
    // turn something on they never got to see the result of. The far side's prompt simply times out.
    val dismiss = {
        vm.cancelEnable()
        onDismiss()
    }

    val title = stringResource(R.string.local_auto_sync_settings_title)
    MobileBottomSheet(onDismissRequest = dismiss, title = title) {
        StageSheetHeader(
            title = title,
            subtitle = stringResource(R.string.local_auto_sync_for_device, device.name),
        )
        Column(
            modifier = Modifier
                .heightIn(max = sheetListHeight())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MobileDimens.GapTiny),
        ) {
            // The switch that makes the rest of the sheet mean something. Everything under it is a
            // draft of a schedule that only exists after the far side accepts.
            SyncCheckRow(
                label = stringResource(R.string.local_auto_sync_enable),
                description = when {
                    enableState is LocalAutoSyncViewModel.EnableState.Asking ->
                        stringResource(R.string.local_auto_sync_waiting, device.name)
                    config.enabled -> null
                    else -> stringResource(R.string.local_auto_sync_enable_hint)
                },
                checked = config.enabled,
                onToggle = { on ->
                    if (on) vm.enable(device, chosenTiming) else vm.disable(device.id)
                },
                enabled = !asking,
            )
            when (val state = enableState) {
                is LocalAutoSyncViewModel.EnableState.Declined ->
                    SyncDangerNote(stringResource(R.string.local_auto_sync_declined, state.deviceName))
                is LocalAutoSyncViewModel.EnableState.Failed ->
                    SyncDangerNote(stringResource(state.reason.messageRes()))
                else -> Unit
            }
            // The other side of the same agreement: this device accepted that one. Shown where the
            // schedules are, because it is the only place the user can take it back.
            if (device.id in consents) {
                SyncNote(stringResource(R.string.local_auto_sync_allowed_by, device.name))
                StageMenuItem(
                    stringResource(R.string.local_auto_sync_revoke),
                    onClick = { vm.revokeConsent(device.id) },
                    icon = MobileIcons.Close,
                    danger = true,
                    enabled = !asking,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            StageGroupLabel(stringResource(R.string.local_auto_sync_what_to_sync))
            // The same everything-row the sync sheet has, because it is the same list: seven rows is
            // a long enough list that "all of it" deserves one tap of its own.
            SyncCheckRow(
                label = stringResource(R.string.local_sync_everything),
                description = null,
                checked = chosenSections.size == BackupManager.Section.entries.size,
                onToggle = { on -> sections = if (on) BackupManager.Section.entries.toSet() else emptySet() },
                enabled = editable,
            )
            BackupManager.Section.entries.forEach { section ->
                SyncCheckRow(
                    label = stringResource(section.labelRes()),
                    description = stringResource(section.descriptionRes()),
                    checked = section in chosenSections,
                    onToggle = { on ->
                        sections = if (on) chosenSections + section else chosenSections - section
                    },
                    enabled = editable,
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
                        enabled = editable,
                    )
                }
            }
            StageGroupLabel(stringResource(R.string.local_auto_sync_when))
            // No "Never" row: the switch above is the off, and two places to turn the same thing
            // off would let them disagree.
            LocalAutoSyncTiming.entries.filter { it != LocalAutoSyncTiming.OFF }.forEach { option ->
                StageSettingRow(
                    title = stringResource(option.labelRes()),
                    minHeight = 48.dp,
                    onClick = if (editable) ({ timing = option }) else null,
                    modifier = Modifier.alpha(if (editable) 1f else 0.45f),
                ) { StageRadio(selected = option == chosenTiming) }
            }
            // What the last run with this device did, when there was one. A setting that says
            // "daily" and a phone that has not run in a week are both true at once, and only one of
            // them is on screen here — so the other one has to be.
            if (config.lastRunAt > 0L) {
                SyncNote(stringResource(R.string.local_auto_sync_last_run, lastRunText(config.lastRunAt)))
            }
            SyncNote(stringResource(R.string.local_auto_sync_note))
        }
        SyncButtons(
            confirm = stringResource(R.string.local_auto_sync_save),
            // Nothing can be saved until there is a schedule to save into — the switch is the only
            // thing that creates one. After that, nothing ticked means nothing to sync: the button
            // says so rather than saving a schedule that would fetch a whole backup to do nothing
            // with it.
            confirmEnabled = canSave,
            onConfirm = {
                vm.save(
                    deviceId = device.id,
                    timing = chosenTiming,
                    sections = chosenSections,
                    // Everything ticked is saved as "all", not as a list of today's ids: a viewer
                    // added next week belongs in a schedule that says "all of them".
                    profileIds = if (chosenProfiles.size == allProfiles.size) emptySet() else chosenProfiles,
                )
                onDismiss()
            },
            onDismiss = dismiss,
        )
    }
}

/** The word on the "When" rows, and on the paired device's row when it has a schedule. */
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
