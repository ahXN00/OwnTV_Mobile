package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.SectionHeader
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The settings root: a field to search all of it, the switches the user pinned, and the nine groups.
 *
 * The TV app puts every setting behind a two-column browser on one enormous screen. A phone gets a
 * plain list instead — nine rows, each opening a page — because a thumb scrolls faster than it aims,
 * and because search makes the depth cheap: typing "wifi" reaches a row four taps down in one.
 */
@Composable
fun SettingsScreen(
    onOpenRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val entries = rememberSettingsSearchEntries()
    val results = entries.matching(query)
    val pinnedKeys = vm.settings.quickPinnedKeys.pref(emptyList())
    // Only the keys this app has a row for. The rest are the TV app's, kept in the stored list so
    // switching back to the television finds them where they were left.
    val pinned = pinnedKeys.mapNotNull { key -> QUICK_TOGGLES.firstOrNull { it.key == key } }

    SettingsPage(modifier) {
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.settings_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MobileDimens.ScreenPaddingH,
                        vertical = MobileDimens.GapSmall,
                    ),
            )
        }

        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        text = stringResource(R.string.search_no_results, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            horizontal = MobileDimens.ScreenPaddingH,
                            vertical = MobileDimens.GapMedium,
                        ),
                    )
                }
            } else {
                item(key = "results-header") {
                    SectionHeader(title = stringResource(R.string.settings_results_title))
                }
                items(results, key = { "${it.route}-${it.title}" }) { entry ->
                    MobileListRow(
                        title = entry.title,
                        subtitle = settingsBreadcrumb(entry),
                        leading = { Icon(entry.group.icon, contentDescription = null) },
                        onClick = { onOpenRoute(entry.route) },
                    )
                }
            }
            return@SettingsPage
        }

        // Quick stays even when nothing is pinned, and says so. The section vanishing was worse: the
        // user who unpinned their last row had no way of telling that Quick still existed.
        item(key = "quick-header") {
            SectionHeader(title = stringResource(R.string.settings_group_quick))
        }
        if (pinned.isEmpty()) {
            item(key = "quick-empty") {
                MobileListRow(
                    title = stringResource(R.string.settings_quick_empty_title),
                    // The television's own hint names the OK button. A phone has none.
                    subtitle = stringResource(R.string.settings_quick_empty_hint_touch),
                )
            }
        } else {
            items(pinned, key = { it.key }) { toggle -> QuickSwitchRow(vm = vm, toggle = toggle) }
        }
        item(key = "quick-divider") {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        items(SettingsGroup.entries, key = { it.route }) { group ->
            SettingRow(
                title = stringResource(group.titleRes),
                subtitle = stringResource(group.summaryRes),
                showChevron = true,
                onClick = { onOpenRoute(group.route) },
            )
        }
    }
}
