package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.database.entity.CategoryEntity
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.FilterChipRow
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.MobileTextField

/**
 * Hide, rename and reorder a section's folders — and bring back anything hidden.
 *
 * Individual channels and films are hidden and renamed from their own long-press menu, where the
 * item already is; this page is the other half of that, because a hidden item has no row left to
 * long-press and would otherwise be gone for good.
 */
@Composable
fun SettingsCustomizePage(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    val type = SECTIONS[tab]
    val categories = remember(type) { vm.categories(type) }.pref(emptyList())
    val custom = remember(type) { vm.customizations(type) }.pref(SectionCustomizations())
    var renaming by remember { mutableStateOf<Pair<String, String>?>(null) }

    val ordered = categories.ordered(custom)

    SettingsPage(modifier) {
        item(key = "tabs") {
            FilterChipRow(
                labels = SECTIONS.map { stringResource(it.labelRes()) },
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }

        settingsSection(R.string.settings_customize_categories)
        if (ordered.isEmpty()) {
            settingsNote(R.string.settings_customize_empty)
        }
        items(ordered, key = { "cat-${CustomizeKeys.category(it)}" }) { category ->
            val catKey = CustomizeKeys.category(category)
            val index = ordered.indexOf(category)
            MobileListRow(
                title = custom.categoryNames[catKey] ?: category.name,
                subtitle = stringResource(R.string.content_rename),
                onClick = { renaming = catKey to (custom.categoryNames[catKey] ?: category.name) },
                trailing = {
                    Row {
                        MoveCategoryButton(up = true, enabled = index > 0) {
                            vm.customizeEdit(type) { pid, t ->
                                setCategoryOrder(pid, t, ordered.movedKeys(index, -1))
                            }
                        }
                        MoveCategoryButton(up = false, enabled = index < ordered.lastIndex) {
                            vm.customizeEdit(type) { pid, t ->
                                setCategoryOrder(pid, t, ordered.movedKeys(index, 1))
                            }
                        }
                        Switch(
                            checked = catKey !in custom.hiddenCategories,
                            onCheckedChange = { show ->
                                vm.customizeEdit(type) { pid, t ->
                                    setCategoryHidden(pid, t, catKey, !show)
                                }
                            },
                        )
                    }
                },
            )
        }

        settingsSection(type.hiddenTitleRes())
        settingsNote(R.string.settings_customize_unhide_description)
        items(custom.hiddenItems.entries.toList(), key = { "hidden-${it.key}" }) { entry ->
            MobileListRow(
                title = entry.value,
                trailing = {
                    TextButton(
                        onClick = {
                            vm.customizeEdit(type) { pid, t ->
                                setItemHidden(pid, t, entry.key, entry.value, false)
                            }
                        },
                    ) { Text(stringResource(R.string.common_show)) }
                },
            )
        }
    }

    renaming?.let { (catKey, current) ->
        RenameDialog(
            initial = current,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                vm.customizeEdit(type) { pid, t ->
                    renameCategory(pid, t, catKey, name.takeIf { it.isNotBlank() })
                }
                renaming = null
            },
        )
    }
}

/** A blank name restores the provider's own, which is the only way back once one is set. */
@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.content_rename)) },
        text = {
            MobileTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.content_rename),
                supportingText = stringResource(R.string.content_rename_hint),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun MoveCategoryButton(up: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = if (up) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(
                if (up) R.string.settings_row_menu_move_up else R.string.settings_row_menu_move_down,
            ),
        )
    }
}

/** The three sections that have folders to arrange. Downloads and the guide take theirs from these. */
private val SECTIONS = listOf(MediaType.LIVE, MediaType.MOVIE, MediaType.SERIES)

/**
 * Hidden folders included, in the user's saved order — the browse screens drop the hidden ones, but
 * this page is where they are switched back on.
 */
private fun List<CategoryEntity>.ordered(c: SectionCustomizations): List<CategoryEntity> {
    if (c.categoryOrder.isEmpty()) return this
    val index = c.categoryOrder.withIndex().associate { (i, k) -> k to i }
    val (pinned, rest) = partition { CustomizeKeys.category(it) in index }
    return pinned.sortedBy { index.getValue(CustomizeKeys.category(it)) } + rest
}

private fun List<CategoryEntity>.movedKeys(index: Int, delta: Int): List<String> =
    map { CustomizeKeys.category(it) }.toMutableList().apply { add(index + delta, removeAt(index)) }

private fun MediaType.labelRes() = when (this) {
    MediaType.MOVIE -> R.string.common_nav_movies
    MediaType.SERIES -> R.string.common_nav_series
    else -> R.string.common_nav_live_tv
}

private fun MediaType.hiddenTitleRes() = when (this) {
    MediaType.MOVIE -> R.string.settings_customize_hidden_movies
    MediaType.SERIES -> R.string.settings_customize_hidden_series
    else -> R.string.settings_customize_hidden_channels
}
