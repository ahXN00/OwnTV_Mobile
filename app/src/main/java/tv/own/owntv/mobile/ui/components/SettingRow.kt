package tv.own.owntv.mobile.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * One line of a settings screen, in the three shapes the TV app's settings use: a switch, a
 * current value, or a chevron into a sub-screen. Built on [MobileListRow] so a settings row is
 * the same height and the same long-press target as every other list row in the app.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    checked: Boolean? = null,
    value: String? = null,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    MobileListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = {
            if (!enabled) return@MobileListRow
            // Tapping anywhere on a switch row toggles it — the switch itself is a 32 dp target.
            if (checked != null && onCheckedChange != null) onCheckedChange(!checked) else onClick()
        },
        trailing = when {
            checked != null -> {
                {
                    Switch(
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        enabled = enabled,
                    )
                }
            }
            value != null -> {
                {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            showChevron -> {
                {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> null
        },
    )
}
