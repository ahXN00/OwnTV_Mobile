package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.glassClickable
import tv.own.owntv.mobile.ui.theme.glassSurface

/** A chip is a pill, at both settings. */
private val ChipShape = RoundedCornerShape(percent = 50)

/** Short enough to stay a strip, tall enough to be a comfortable target. */
private val ChipHeight = 40.dp

/**
 * The horizontal filter strip above a list — categories on Live, genres in the library.
 *
 * Selection is explicit and momentary: there is no "currently focused" chip to keep track of,
 * only the one the user chose. The chosen chip wears the accent on its rim and holds it; a chip
 * being pressed sinks under the finger and ticks. Neither draws a ripple, which over glass reads
 * as a smear rather than as a touch.
 */
@Composable
fun FilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = MobileDimens.ScreenPaddingH,
                vertical = MobileDimens.GapSmall,
            ),
        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
    ) {
        labels.forEachIndexed { index, label ->
            val chosen = index == selectedIndex
            val press = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = ChipHeight)
                    .glassSurface(
                        surface = GlassSurface.CARDS,
                        shape = ChipShape,
                        interactionSource = press,
                        selected = chosen,
                    )
                    .clip(ChipShape)
                    .glassClickable(press, onClick = { onSelect(index) })
                    .padding(horizontal = MobileDimens.GapMedium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (chosen) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
