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
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.sheetListHeight
import tv.own.owntv.mobile.ui.stage.StageGroupLabel
import tv.own.owntv.mobile.ui.stage.StageRadio
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The auto-sync settings: what travels, and when it travels by itself.
 *
 * The shape is the one the television app uses — two lists under one heading, and a Save button that
 * is the only thing that makes anything real. Nothing here changes the saved settings as it is
 * touched: the sheet edits a draft, and only Save writes it down. A phone that saved as it ticked
 * would leave a half-finished schedule behind every time the user swiped the sheet away.
 *
 * The two lists are the same two questions the sync and backup sheets ask — which parts of the
 * data, and how often — so the rows say what the rest of the app already says for them.
 */
@Composable
fun LocalAutoSyncSheet(vm: LocalAutoSyncViewModel, onDismiss: () -> Unit) {
    val config by vm.config.collectAsStateWithLifecycle()
    // The draft. Null means "not touched yet", so the sheet shows what is saved until the user
    // changes something — the settings arrive from disk a moment after the sheet opens.
    var timing by remember { mutableStateOf<LocalAutoSyncTiming?>(null) }
    var sections by remember { mutableStateOf<Set<BackupManager.Section>?>(null) }
    val chosenTiming = timing ?: config.timing
    val chosenSections = sections ?: config.sections

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
            confirmEnabled = chosenSections.isNotEmpty(),
            onConfirm = {
                vm.save(chosenTiming, chosenSections)
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
