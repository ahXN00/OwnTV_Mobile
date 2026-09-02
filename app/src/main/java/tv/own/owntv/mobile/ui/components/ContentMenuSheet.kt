package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import tv.own.owntv.core.menu.applyMenuOrder
import tv.own.owntv.core.model.ContentMenu
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * One action in a long-press content menu.
 *
 * [key] is the stable identifier the saved order is written in, and it must match the key core's
 * catalogue uses for the same action — that catalogue is what the settings screen arranges, on
 * either app. [group] only spaces the sheet out: a divider is drawn wherever it changes.
 */
data class SheetAction(
    val key: String,
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val group: Int = 0,
    val onClick: () -> Unit,
)

/**
 * The long-press menu for a channel, movie, series or episode.
 *
 * The TV app draws these as a focused column in the middle of the screen; here they rise as a
 * bottom sheet, because a phone's actions belong under a thumb. What the two share is the order:
 * both read the same saved arrangement and apply it with core's rule, so a user who moved
 * "Download" to the top of the movie menu on the television finds it at the top here too.
 *
 * Tapping an action dismisses the sheet — every one of them either navigates or changes the row
 * being looked at, and a sheet left open over the result is a sheet the user has to close.
 */
@Composable
fun ContentMenuSheet(
    menu: ContentMenu,
    title: String,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings: SettingsRepository = koinInject()
    val order by remember(menu) { settings.menuOrder(menu.name.lowercase()) }
        .collectAsStateWithLifecycle(emptyList())

    MobileBottomSheet(onDismissRequest = onDismiss, title = title, modifier = modifier) {
        var previousGroup: Int? = null
        applyMenuOrder(actions, order) { it.key }.forEach { action ->
            if (previousGroup != null && action.group != previousGroup) HorizontalDivider()
            previousGroup = action.group
            SheetActionRow(action = action, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun SheetActionRow(action: SheetAction, onDismiss: () -> Unit) {
    val color =
        if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MobileDimens.ListRowHeight)
            .clickable { action.onClick(); onDismiss() }
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(MobileDimens.ListRowIconSize), contentAlignment = Alignment.Center) {
            if (action.icon != null) {
                Icon(imageVector = action.icon, contentDescription = null, tint = color)
            }
        }
        Spacer(Modifier.width(MobileDimens.GapMedium))
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
