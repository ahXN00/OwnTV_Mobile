package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.ui.theme.MobileCardShape
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.glassClickable
import tv.own.owntv.mobile.ui.theme.glassSurface

/**
 * One tappable line in a list — a channel, a profile, a download.
 *
 * Long-press is how a touch user reaches the actions the TV app puts behind a context menu, so it
 * is part of the row rather than something each screen adds. A row without [onLongClick] simply
 * has no menu.
 */
@Composable
fun MobileListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleMaxLines: Int = 1,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val press = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MobileDimens.ListRowHeight)
            // Inline glass: a row is darker than the panel holding it, which is what groups a list
            // into rows instead of leaving text floating on a page.
            .glassSurface(GlassSurface.CARDS, MobileCardShape, interactionSource = press)
            .clip(MobileCardShape)
            .glassClickable(press, onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = MobileDimens.ScreenPaddingH,
                vertical = MobileDimens.GapSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.size(MobileDimens.ListRowIconSize),
                contentAlignment = Alignment.Center,
                content = { leading() },
            )
            Spacer(Modifier.width(MobileDimens.GapMedium))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Box(Modifier.padding(start = MobileDimens.GapSmall)) { trailing() }
        }
    }
}
