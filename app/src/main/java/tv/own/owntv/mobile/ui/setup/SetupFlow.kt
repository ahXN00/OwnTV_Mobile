package tv.own.owntv.mobile.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.setup.SourceImporter
import tv.own.owntv.core.setup.displayText
import tv.own.owntv.core.sync.detailText
import tv.own.owntv.core.sync.importProgressDisplay
import tv.own.owntv.core.sync.primaryText
import tv.own.owntv.core.sync.remainderText
import tv.own.owntv.core.sync.summaryText
import tv.own.owntv.core.sync.warningText
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileButton
import tv.own.owntv.mobile.ui.components.MobileButtonStyle
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.MobileTextField
import tv.own.owntv.mobile.ui.theme.MobileDimens

private enum class Step { CHOICE, FORM, IMPORTING, RESTORE }

/**
 * Getting content onto the phone: type a playlist in, or bring everything back from a backup.
 *
 * The same flow serves both the empty first run and "add a playlist" later on — the only difference
 * is what happens at the end, which is [onDone]'s business, and whether there is anywhere to go
 * back to.
 */
@Composable
fun SetupFlow(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    val vm: SetupViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(Step.CHOICE) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickBackup = rememberBackupFilePicker { uri ->
        scope.launch {
            copyPickedFile(context, uri, context.cacheDir)?.let(vm::importBackup)
        }
    }

    // Back out of a step rather than out of the app; from the first step there is nowhere to go
    // unless the caller says so, and an install with no playlist has nothing behind it.
    BackHandler(enabled = step != Step.CHOICE || onCancel != null) {
        when (step) {
            Step.CHOICE -> onCancel?.invoke()
            Step.FORM, Step.RESTORE -> { vm.reset(); step = Step.CHOICE }
            Step.IMPORTING -> Unit // the buttons on that screen decide; a stray swipe must not abandon a sync
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (step) {
            Step.CHOICE -> SetupChoice(
                onAddSource = { step = Step.FORM },
                onRestore = { step = Step.RESTORE; pickBackup() },
                onCancel = onCancel,
            )
            Step.FORM -> AddSourceForm(
                onStartXtream = { name, server, user, pass, ua, refresh, live, movies, series, hls ->
                    vm.startXtream(name, server, user, pass, ua, refresh, live, movies, series, hls)
                    step = Step.IMPORTING
                },
                onStartM3u = { name, url, ua, refresh ->
                    vm.startM3u(name, url, ua, refresh)
                    step = Step.IMPORTING
                },
                onStartStalker = { name, portal, mac, serial, dev1, dev2, sig, ua, refresh, live, movies, series ->
                    vm.startStalker(name, portal, mac, serial, dev1, dev2, sig, ua, refresh, live, movies, series)
                    step = Step.IMPORTING
                },
            )
            Step.IMPORTING -> ImportProgress(
                state = state,
                progressText = progress?.importProgressDisplay(),
                onContinue = { vm.finish(onDone) },
                onRetry = { vm.reset(); step = Step.FORM },
                onCancel = { vm.cancelImport(); step = Step.FORM },
            )
            Step.RESTORE -> RestoreBackup(
                state = state,
                onPassword = vm::restoreWithPassword,
                onContinue = { vm.finish(onDone) },
                onPickAgain = { vm.reset(); pickBackup() },
                onBack = { vm.reset(); step = Step.CHOICE },
            )
        }
    }
}

@Composable
private fun SetupChoice(onAddSource: () -> Unit, onRestore: () -> Unit, onCancel: (() -> Unit)?) {
    SetupPage {
        Text(
            text = stringResource(R.string.setup_set_up_owntv),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.setup_setup_choice_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MobileDimens.GapSmall))
        MobileListRow(
            title = stringResource(R.string.setup_add_playlist),
            subtitle = stringResource(R.string.setup_add_playlist_description),
            leading = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
            onClick = onAddSource,
        )
        MobileListRow(
            title = stringResource(R.string.setup_restore_backup),
            subtitle = stringResource(R.string.setup_import_profiles_playlists),
            leading = { Icon(Icons.Filled.Restore, contentDescription = null) },
            onClick = onRestore,
        )
        if (onCancel != null) {
            MobileButton(
                text = stringResource(R.string.common_cancel),
                onClick = onCancel,
                style = MobileButtonStyle.TEXT,
            )
        }
    }
}

/** The import, from the first request to "All set!" or the reason it stopped. */
@Composable
private fun ImportProgress(
    state: SourceImporter.ImportState,
    progressText: tv.own.owntv.core.sync.SyncProgressDisplay?,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val resources = LocalContext.current.resources
    SetupPage {
        when (state) {
            is SourceImporter.ImportState.Success -> {
                Text(
                    text = stringResource(R.string.setup_all_set),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                state.counts?.let { Detail(it.summaryText(resources, includeEpg = true)) }
                state.warnings.warningText(resources)?.let { Detail(it) }
                state.remainder.remainderText(resources)?.let { Detail(it) }
                MobileButton(
                    text = stringResource(R.string.setup_continue),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is SourceImporter.ImportState.Failed -> {
                Text(
                    text = stringResource(R.string.setup_import_failed),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Detail(state.failure.displayText(resources))
                Row(horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall)) {
                    MobileButton(
                        text = stringResource(R.string.common_back),
                        onClick = onCancel,
                        style = MobileButtonStyle.SECONDARY,
                    )
                    MobileButton(text = stringResource(R.string.setup_try_again_caps), onClick = onRetry)
                }
            }
            else -> {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.setup_importing_catalog),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = progressText?.primaryText(resources)
                        ?: stringResource(R.string.setup_preparing_catalog),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Detail(
                    progressText?.detailText(resources)
                        ?: stringResource(R.string.setup_preparing_catalog),
                )
                MobileButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onCancel,
                    style = MobileButtonStyle.SECONDARY,
                )
            }
        }
    }
}

/** The restore, once a file has been picked: its password if it needs one, then the result. */
@Composable
private fun RestoreBackup(
    state: SourceImporter.ImportState,
    onPassword: (java.io.File, String?) -> Unit,
    onContinue: () -> Unit,
    onPickAgain: () -> Unit,
    onBack: () -> Unit,
) {
    val resources = LocalContext.current.resources
    SetupPage {
        when (state) {
            is SourceImporter.ImportState.NeedPassword -> {
                var password by remember(state.file, state.retry) { mutableStateOf("") }
                Text(
                    text = stringResource(
                        if (state.retry) R.string.setup_wrong_backup_password else R.string.setup_enter_backup_password,
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Detail(
                    stringResource(
                        when {
                            state.retry && state.sealed -> R.string.setup_password_mismatch_sealed
                            state.retry -> R.string.setup_password_mismatch
                            state.sealed -> R.string.setup_backup_encrypted_prompt
                            else -> R.string.setup_backup_passwords_encrypted_prompt
                        },
                    ),
                )
                MobileTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.setup_backup_password),
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall)) {
                    MobileButton(
                        text = stringResource(R.string.common_back),
                        onClick = onBack,
                        style = MobileButtonStyle.SECONDARY,
                    )
                    // A sealed container is nothing but ciphertext: skipping the password would
                    // restore an empty backup, so that way out is not offered.
                    if (!state.sealed) {
                        MobileButton(
                            text = stringResource(R.string.setup_skip_no_passwords),
                            onClick = { onPassword(state.file, null) },
                            style = MobileButtonStyle.SECONDARY,
                        )
                    }
                    MobileButton(
                        text = stringResource(R.string.setup_restore),
                        onClick = { onPassword(state.file, password) },
                        enabled = password.isNotBlank(),
                    )
                }
            }
            is SourceImporter.ImportState.Success -> {
                Text(
                    text = stringResource(R.string.setup_all_set),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                state.restoredItems?.let {
                    Detail(pluralStringResource(R.plurals.setup_restored_items, it, it))
                }
                if (state.passwordsOmitted) Detail(stringResource(R.string.setup_passwords_omitted))
                state.skippedSources.takeIf { it > 0 }?.let {
                    Detail(pluralStringResource(R.plurals.setup_skipped_sources, it, it))
                }
                if (state.invalidLocale) Detail(stringResource(R.string.setup_invalid_locale))
                MobileButton(
                    text = stringResource(R.string.setup_continue),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is SourceImporter.ImportState.Failed -> {
                Text(
                    text = stringResource(R.string.setup_restore_failed),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Detail(state.failure.displayText(resources))
                Row(horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall)) {
                    MobileButton(
                        text = stringResource(R.string.common_back),
                        onClick = onBack,
                        style = MobileButtonStyle.SECONDARY,
                    )
                    MobileButton(text = stringResource(R.string.setup_try_again_caps), onClick = onPickAgain)
                }
            }
            else -> {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.setup_restoring),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                // The picker is a separate activity: cancelling it leaves this screen with nothing
                // happening, so there is always a way back and a way to pick another file.
                Row(horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall)) {
                    MobileButton(
                        text = stringResource(R.string.common_back),
                        onClick = onBack,
                        style = MobileButtonStyle.SECONDARY,
                    )
                    MobileButton(
                        text = stringResource(R.string.setup_pick_backup_file),
                        onClick = onPickAgain,
                        style = MobileButtonStyle.SECONDARY,
                    )
                }
            }
        }
    }
}

/** One column, centred, scrolling — every step of this flow is short enough to fit but must still
 *  survive a keyboard and a small screen in landscape. */
@Composable
private fun SetupPage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapLarge),
        verticalArrangement = Arrangement.spacedBy(MobileDimens.GapMedium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun Detail(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
