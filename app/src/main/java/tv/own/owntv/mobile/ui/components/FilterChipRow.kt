package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The horizontal filter strip above a list — categories on Live, genres in the library.
 *
 * Selection is explicit and momentary: there is no "currently focused" chip to keep track of,
 * only the one the user chose.
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
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = {
                    Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}
