package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * Find a category by typing, instead of dragging the chip strip past four hundred of them.
 *
 * A playlist with hundreds of categories makes the horizontal rail useless on a phone — the one on
 * the television at least has a rail tall enough to page through. Picking one closes the sheet.
 */
@Composable
fun CategoryPickerSheet(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(labels, query) {
        labels.withIndex().filter { (_, label) -> label.contains(query.trim(), ignoreCase = true) }
    }
    MobileBottomSheet(onDismissRequest = onDismiss) {
        MobileTextField(
            value = query,
            onValueChange = { query = it },
            label = stringResource(R.string.content_search_categories),
            modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
        )
        LazyColumn(Modifier.heightIn(max = sheetListHeight())) {
            itemsIndexed(matches, key = { _, (index, _) -> index }) { _, (index, label) ->
                MobileListRow(
                    title = label,
                    trailing = if (index == selectedIndex) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onSelect(index)
                        onDismiss()
                    },
                )
            }
        }
    }
}
