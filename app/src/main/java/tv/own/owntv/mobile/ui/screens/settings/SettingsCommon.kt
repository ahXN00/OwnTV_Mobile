package tv.own.owntv.mobile.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.SectionHeader
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The nine settings groups, in the order the TV app's root list puts them.
 *
 * Each is a route of its own rather than an expanding block: a phone screen holds far less than a
 * television's, and a back gesture out of a page is cheaper than scrolling past eight collapsed
 * groups to reach the ninth. Quick is not here — it lives inline at the top of the root.
 */
enum class SettingsGroup(
    val route: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val summaryRes: Int,
    val icon: ImageVector,
) {
    PROFILE("settings/profile", R.string.settings_profile_group, R.string.settings_group_summary_profile, Icons.Filled.Person),
    SOURCES("settings/sources", R.string.settings_group_sources, R.string.settings_group_summary_sources, Icons.Filled.PlaylistPlay),
    APPEARANCE("settings/appearance", R.string.settings_appearance_group, R.string.settings_group_summary_appearance, Icons.Filled.Palette),
    LAYOUT("settings/layout", R.string.settings_group_layout, R.string.settings_group_summary_layout, Icons.Filled.ViewList),
    CONTENT("settings/content", R.string.settings_group_content_metadata, R.string.settings_group_summary_content_metadata, Icons.Filled.Image),
    PLAYBACK("settings/playback", R.string.settings_playback_group, R.string.settings_group_summary_playback, Icons.Filled.PlayCircle),
    NETWORK("settings/network", R.string.settings_network_group, R.string.settings_group_summary_network, Icons.Filled.Wifi),
    DATA("settings/data", R.string.settings_group_data, R.string.settings_group_summary_data, Icons.Filled.Storage),
    APP("settings/app", R.string.settings_app_group, R.string.settings_group_summary_app, Icons.Filled.Info),
}

/** The group a settings route belongs to, so the shell's bar can name the page and offer back. */
fun settingsGroupOf(route: String?): SettingsGroup? =
    SettingsGroup.entries.firstOrNull { it.route == route }

/**
 * The bar's title for a settings page, or null when the route is not one — the nine group pages and
 * every leaf under them. A non-null answer is also what tells the bar to offer back.
 *
 * Leaves are asked first: a leaf route begins with its group's route, so testing the group first
 * would title every leaf after the group it hangs off.
 */
@StringRes
fun settingsPageTitleRes(route: String?): Int? =
    settingsLeafOf(route)?.titleRes ?: settingsGroupOf(route)?.titleRes

/** Collect a settings flow for the row that displays it. */
@Composable
fun <T> Flow<T>.pref(initial: T): T = collectAsStateWithLifecycle(initial).value

/** The scrolling body every group page shares. */
@Composable
fun SettingsPage(modifier: Modifier = Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = MobileDimens.GapLarge,
        ),
        content = content,
    )
}

/** A heading inside a group page — "Mobile", "Subtitles", "Guide". */
fun LazyListScope.settingsSection(@StringRes titleRes: Int) {
    item(key = "section-$titleRes") {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            SectionHeader(title = stringResource(titleRes))
        }
    }
}

/** The rows that open a group's screen-sized settings, at the head of its page. */
fun LazyListScope.settingsLeafRows(group: SettingsGroup, onOpen: (SettingsLeaf) -> Unit) {
    items(leavesOf(group), key = { it.route }) { leaf ->
        SettingRow(
            title = stringResource(leaf.titleRes),
            subtitle = leaf.summaryRes?.let { stringResource(it) },
            showChevron = true,
            onClick = { onOpen(leaf) },
        )
    }
}

/** Explanatory text under a heading, for the settings that need a sentence rather than a subtitle. */
fun LazyListScope.settingsNote(@StringRes textRes: Int) {
    item(key = "note-$textRes") {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = MobileDimens.ScreenPaddingH,
                vertical = MobileDimens.GapSmall,
            ),
        )
    }
}

/**
 * A percentage a finger drags, rather than a value a remote steps through. The label shows the live
 * value while the thumb moves, so the user is never guessing what they are about to commit to.
 */
@Composable
fun SettingsSlider(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueLabel: String = stringResource(R.string.common_percent, value),
    steps: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps,
        )
    }
}

/** One option in a [SettingsChoiceSheet]. */
data class SettingsChoice<T>(val value: T, val label: String, val description: String? = null)

/**
 * The bottom sheet that replaces the TV app's centred picker dialogs — theme, accent, fonts, zoom
 * and every other "one of these" setting. Choosing dismisses it; there is no confirm button.
 */
@Composable
fun <T> SettingsChoiceSheet(
    title: String,
    choices: List<SettingsChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    MobileBottomSheet(onDismissRequest = onDismiss, title = title) {
        choices.forEach { choice ->
            MobileListRow(
                title = choice.label,
                subtitle = choice.description,
                onClick = { onSelect(choice.value); onDismiss() },
                trailing = if (choice.value == selected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 0.dp),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
