package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** A sheet's list may take at most half the screen: the buttons under it have to stay reachable. */
@Composable
fun sheetListHeight() = (LocalConfiguration.current.screenHeightDp / 2).dp

/**
 * The bottom sheet every long-press menu and picker in this app uses.
 *
 * It replaces the TV app's centred popups: a sheet rises into the bottom third of the screen, where
 * a thumb already is, and the system back gesture or a tap outside dismisses it.
 *
 * Calling this does not draw anything here — it hands the sheet to [MobileSheetHost], which draws it
 * at the top of the app's own window. That is what lets it frost the same wallpaper the rest of the
 * app frosts; a separate dialog window cannot.
 */
@Composable
fun MobileBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val host = checkNotNull(LocalSheetHost.current) {
        "MobileBottomSheet needs a MobileSheetHost above it"
    }
    val entry = remember { SheetEntry(onDismissRequest, modifier, title, content) }
    // The caller recomposes with new lambdas and a new title on every frame it feels like; the entry
    // is the same open sheet throughout, so it is updated in place rather than replaced.
    SideEffect {
        entry.onDismissRequest = onDismissRequest
        entry.modifier = modifier
        entry.title = title
        entry.content = content
    }
    DisposableEffect(host) {
        host.entries.add(entry)
        onDispose { host.entries.remove(entry) }
    }
}
