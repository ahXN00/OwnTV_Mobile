package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import tv.own.owntv.mobile.ui.theme.MobileDimens

/**
 * The bottom sheet every long-press menu and picker in this app uses.
 *
 * It replaces the TV app's centred popups: a sheet rises into the bottom third of the screen,
 * where a thumb already is, and the system back gesture or a tap outside dismisses it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = MobileDimens.SheetCorner,
            topEnd = MobileDimens.SheetCorner,
        ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    horizontal = MobileDimens.ScreenPaddingH,
                    vertical = MobileDimens.GapSmall,
                ),
            )
        }
        content()
        // The gesture bar sits over the sheet's last row otherwise.
        androidx.compose.foundation.layout.Spacer(
            Modifier
                .navigationBarsPadding()
                .padding(bottom = MobileDimens.GapSmall),
        )
    }
}
