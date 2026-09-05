package tv.own.owntv.mobile.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.SharedFlow
import tv.own.owntv.core.theme.AnimationLevel
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.nav.MobileDestination
import java.text.NumberFormat
import tv.own.owntv.mobile.ui.theme.LocalAnimations

/**
 * A tab that does not exist yet.
 *
 * It is a long list on purpose: the shell's scroll restoration and its long-press-to-top are only
 * testable against something that actually scrolls. Each row is the tab's own name and a number, so
 * the screen carries no text of its own — it is deleted by the phase that builds the real tab.
 */
@Composable
fun PlaceholderScreen(
    destination: MobileDestination,
    scrollToTop: SharedFlow<String>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    listState.ObeyScrollToTop(route = destination.route, scrollToTop = scrollToTop)

    val label = stringResource(destination.labelRes)
    val numbers = NumberFormat.getIntegerInstance()

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(count = 40) { index ->
            MobileListRow(title = label, subtitle = numbers.format(index + 1))
            HorizontalDivider()
        }
    }
}

/** Jumps this list to the top when the shell says its tab was long-pressed. */
@Composable
fun LazyListState.ObeyScrollToTop(route: String, scrollToTop: SharedFlow<String>) {
    val instant = LocalAnimations.current == AnimationLevel.OFF
    LaunchedEffect(route, instant) {
        scrollToTop.collect { requested ->
            if (requested != route) return@collect
            if (instant) scrollToItem(0) else animateScrollToItem(0)
        }
    }
}
