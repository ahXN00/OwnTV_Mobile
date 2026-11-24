package tv.own.owntv.mobile.ui.screens.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.skip.SkipButtonShow
import tv.own.owntv.core.skip.SkipChoices
import tv.own.owntv.core.skip.SkipKind
import tv.own.owntv.core.skip.SkipMode
import tv.own.owntv.core.skip.TheIntroDbAccount
import tv.own.owntv.core.skip.TheIntroDbClient
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.stage.StageButton
import tv.own.owntv.mobile.ui.stage.StageButtonStyle
import tv.own.owntv.mobile.ui.stage.StageChevron
import tv.own.owntv.mobile.ui.stage.StageRowValue
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageTextRow
import tv.own.owntv.mobile.ui.theme.MobileDimens
import java.util.Date

/**
 * Settings → Content & metadata → TheIntroDB: what each part (intro, recap, end credits, preview) does,
 * how long the skip button stays, and the optional TheIntroDB key with its state and today's allowance.
 *
 * The key is typed once and saved deliberately by Check key, as on the TMDB page: a half-typed key is
 * a key that fails.
 */
@Composable
fun SettingsTheIntroDbPage(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    val skipChoices = vm.settings.skipChoices.pref(SkipChoices())
    val storedKey = vm.settings.theIntroDbApiKey.pref("")
    var key by remember(storedKey) { mutableStateOf(storedKey) }
    var skipSheet by remember { mutableStateOf<SkipKind?>(null) }
    var buttonSheet by remember { mutableStateOf(false) }
    val account = org.koin.compose.koinInject<TheIntroDbAccount>()
    val quota by account.quota.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf<TheIntroDbAccount.Status?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val check: () -> Unit = {
        scope.launch {
            checking = true
            status = runCatching { account.check() }.getOrNull()
            checking = false
        }
    }
    // Opening the page checks once, so the key's state and today's allowance are on screen.
    LaunchedEffect(Unit) { check() }

    SettingsPage(modifier) {
        settingsSection(R.string.settings_skip_group) {
            for (kind in SkipKind.entries) {
                StageSettingRow(
                    title = stringResource(kind.titleRes()),
                    subtitle = stringResource(R.string.settings_line_skip_part),
                    onClick = { skipSheet = kind },
                ) { StageRowValue(stringResource(skipChoices.modeOf(kind).labelRes()), StageChevron.DOWN) }
            }
            StageSettingRow(
                title = stringResource(R.string.settings_skip_button_show),
                subtitle = stringResource(R.string.settings_line_skip_button_show),
                onClick = { buttonSheet = true },
            ) { StageRowValue(stringResource(skipChoices.buttonShow.labelRes()), StageChevron.DOWN) }
        }
        settingsNote(R.string.settings_skip_part_description)
        settingsSection(R.string.settings_theintrodb_account) {
            StageTextRow(MobileIcons.Lock, stringResource(R.string.settings_theintrodb_api_key), key, { key = it }, password = true)
            Column(Modifier.padding(horizontal = MobileDimens.ScreenPaddingH)) {
                StageButton(
                    text = stringResource(R.string.settings_theintrodb_check),
                    onClick = { scope.launch { vm.settings.setTheIntroDbApiKey(key); check() } },
                    enabled = !checking,
                    modifier = Modifier.padding(vertical = MobileDimens.GapSmall),
                    style = StageButtonStyle.PRIMARY,
                )
                if (storedKey.isNotBlank()) {
                    StageButton(
                        text = stringResource(R.string.settings_metadata_clear_advanced_title),
                        onClick = { key = ""; scope.launch { vm.settings.setTheIntroDbApiKey(""); check() } },
                        modifier = Modifier.padding(bottom = MobileDimens.GapSmall),
                    )
                }
                TheIntroDbStatus(if (checking) null else status, storedKey, quota)
            }
        }
        settingsNote(R.string.settings_theintrodb_key_steps)
        // The credit, as TheIntroDB publishes it: its badge untouched (never tinted or reshaped) and one line.
        settingsGroup(key = "theintrodb-credit") {
            Column(Modifier.padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall)) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.theintrodb_logo),
                    contentDescription = stringResource(R.string.settings_theintrodb),
                    // The service's own artwork, at its own proportions: never recoloured or reshaped.
                    modifier = Modifier.height(72.dp).aspectRatio(THEINTRODB_LOGO_RATIO),
                )
                Text(
                    text = stringResource(R.string.settings_theintrodb_attribution),
                    style = tv.own.owntv.mobile.ui.stage.stageText(12.5f, 500),
                    color = tv.own.owntv.mobile.ui.stage.LocalStagePalette.current.muted,
                    modifier = Modifier.padding(top = MobileDimens.GapSmall),
                )
                Text(
                    text = stringResource(R.string.settings_theintrodb_contribute),
                    style = tv.own.owntv.mobile.ui.stage.stageText(12.5f, 500),
                    color = tv.own.owntv.mobile.ui.stage.LocalStagePalette.current.muted,
                    modifier = Modifier.padding(top = MobileDimens.GapSmall),
                )
            }
        }
    }

    skipSheet?.let { kind ->
        SettingsChoiceSheet(
            title = stringResource(kind.titleRes()),
            choices = SkipMode.entries.map { SettingsChoice(it, stringResource(it.labelRes())) },
            selected = skipChoices.modeOf(kind),
            onSelect = { picked -> vm.edit { setSkipMode(kind, picked) } },
            onDismiss = { skipSheet = null },
        )
    }
    if (buttonSheet) {
        SettingsChoiceSheet(
            title = stringResource(R.string.settings_skip_button_show),
            choices = SkipButtonShow.entries.map { SettingsChoice(it, stringResource(it.labelRes())) },
            selected = skipChoices.buttonShow,
            onSelect = { picked -> vm.edit { setSkipButtonShow(picked) } },
            onDismiss = { buttonSheet = false },
        )
    }
}

/** The key's state ("Connected · submissions: 3", no key, not accepted…) and today's lookups left. */
@Composable
private fun TheIntroDbStatus(status: TheIntroDbAccount.Status?, storedKey: String, quota: TheIntroDbClient.Quota?) {
    val palette = tv.own.owntv.mobile.ui.stage.LocalStagePalette.current
    val keyLine = when (val k = status?.key) {
        is TheIntroDbClient.KeyCheck.Valid -> stringResource(R.string.settings_theintrodb_connected, k.submissions)
        TheIntroDbClient.KeyCheck.Invalid -> stringResource(R.string.settings_theintrodb_invalid)
        TheIntroDbClient.KeyCheck.Unreachable -> stringResource(R.string.settings_theintrodb_unreachable)
        null -> if (storedKey.isBlank()) stringResource(R.string.settings_theintrodb_no_key) else null
    }
    keyLine?.let {
        Text(
            text = it,
            style = tv.own.owntv.mobile.ui.stage.stageText(13.5f, 500),
            color = if (status?.key is TheIntroDbClient.KeyCheck.Invalid) palette.danger else palette.text,
        )
    }
    val q = quota ?: status?.quota ?: return
    val context = LocalContext.current
    val separator = stringResource(R.string.content_metadata_separator)
    val refill = if (q.resetSeconds > 0) {
        separator + stringResource(R.string.settings_allowance_refills, DateFormat.getTimeFormat(context).format(Date(q.atMs + q.resetSeconds * 1000)))
    } else ""
    Text(
        text = stringResource(R.string.settings_allowance_day) + separator +
            pluralStringResource(R.plurals.settings_allowance_value, q.remaining, q.remaining, q.limit) + refill,
        style = tv.own.owntv.mobile.ui.stage.stageText(13.5f, 500),
        color = palette.text,
        modifier = Modifier.padding(top = 4.dp, bottom = MobileDimens.GapSmall),
    )
}

private fun SkipKind.titleRes() = when (this) {
    SkipKind.INTRO -> R.string.settings_skip_intro
    SkipKind.RECAP -> R.string.settings_skip_recap
    SkipKind.CREDITS -> R.string.settings_skip_credits
    SkipKind.PREVIEW -> R.string.settings_skip_preview
}

private fun SkipMode.labelRes() = when (this) {
    SkipMode.OFF -> R.string.common_off
    SkipMode.BUTTON -> R.string.settings_skip_mode_button
    SkipMode.AUTO -> R.string.settings_skip_mode_auto
}

private fun SkipButtonShow.labelRes() = when (this) {
    SkipButtonShow.START -> R.string.settings_skip_button_start
    SkipButtonShow.WHOLE -> R.string.settings_skip_button_whole
}

/** The logo artwork's width over its height (850 × 1450). */
private const val THEINTRODB_LOGO_RATIO = 850f / 1450f
