package tv.own.owntv.mobile.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import tv.own.owntv.core.theme.AnimationLevel
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.ui.theme.GlassNest
import tv.own.owntv.mobile.ui.theme.LocalAnimations
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.SquircleTopShape
import tv.own.owntv.mobile.ui.theme.glassSurface
import kotlin.math.roundToInt

/** Where a sheet can come to rest. */
private enum class SheetAnchor { HIDDEN, HALF, EXPANDED }

/** How far the content starts below where it ends up. Enough to read as weight, not as a slide. */
private const val SETTLE_DP = 18f

/** How dark the app goes behind an open sheet. */
private const val SCRIM_ALPHA = 0.5f

/** How far the back gesture shrinks the sheet before it lets go of it. */
private const val BACK_SCALE = 0.92f

/** A sheet never spans a tablet: past this it is a panel at the bottom, not a wall. */
private val SheetMaxWidth = 640.dp

/** One open sheet's registration with the host. Mutable, because the caller keeps recomposing. */
@Stable
internal class SheetEntry(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    title: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    var onDismissRequest by mutableStateOf(onDismissRequest)
    var modifier by mutableStateOf(modifier)
    var title by mutableStateOf(title)
    var content by mutableStateOf(content)
}

/** The one place an open sheet lives. Sheets are modal, so there is only ever one. */
@Stable
class SheetHostState internal constructor() {
    internal var entry by mutableStateOf<SheetEntry?>(null)
}

internal val LocalSheetHost = staticCompositionLocalOf<SheetHostState?> { null }

/**
 * The layer every bottom sheet in the app is drawn on, hosted once above the whole UI.
 *
 * A sheet used to be a `ModalBottomSheet`, which is a separate window — and a separate window cannot
 * replay this window's blurred wallpaper, so every long-press menu fell back to a flat, near-opaque
 * fill. Hosting the sheet *inside* the app's own window is what lets it sample the same frost every
 * other pane samples, and it is also what makes a real predictive-back gesture possible.
 */
@Composable
fun MobileSheetHost(content: @Composable () -> Unit) {
    val host = remember { SheetHostState() }
    val entry = host.entry
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                // Neither a screen reader nor the keyboard's focus may wander into the page behind an
                // open sheet — a dialog window used to give both of those for free, and an in-window
                // sheet has to say so itself.
                .then(
                    if (entry == null) {
                        Modifier
                    } else {
                        Modifier
                            .semantics { hideFromAccessibility() }
                            .focusProperties { canFocus = false }
                    },
                ),
        ) {
            CompositionLocalProvider(LocalSheetHost provides host) { content() }
        }
        // Keyed on the entry, so opening a second sheet builds fresh state rather than sliding the
        // old sheet's position into new content.
        if (entry != null) key(entry) { SheetLayer(entry) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.SheetLayer(entry: SheetEntry) {
    val animations = LocalAnimations.current
    val scope = rememberCoroutineScope()
    val state = remember { AnchoredDraggableState(SheetAnchor.HIDDEN) }

    var containerHeight by remember { mutableStateOf(0) }
    var sheetHeight by remember { mutableStateOf(0) }
    var opened by remember { mutableStateOf(false) }

    // Anchors are translations downward from the resting position, so the sheet is simply laid out
    // at the bottom of the screen and pushed off it by its own height.
    LaunchedEffect(sheetHeight, containerHeight) {
        if (sheetHeight <= 0 || containerHeight <= 0) return@LaunchedEffect
        val full = sheetHeight.toFloat()
        val half = (containerHeight / 2f).coerceAtMost(full)
        val hasHalf = full - half > 1f
        state.updateAnchors(
            DraggableAnchors {
                SheetAnchor.EXPANDED at 0f
                // A sheet shorter than half the screen is already at its half, so it has no middle
                // rest to fall to — two names for one position would only make the drag stutter.
                if (hasHalf) SheetAnchor.HALF at full - half
                SheetAnchor.HIDDEN at full
            },
            state.targetValue,
        )
        if (!opened) {
            opened = true
            val landing = if (hasHalf) SheetAnchor.HALF else SheetAnchor.EXPANDED
            if (animations == AnimationLevel.OFF) state.snapTo(landing) else state.animateTo(landing)
        }
    }

    // Coming to rest on HIDDEN is the dismissal, however it got there — a fling, a drag, the scrim or
    // the back gesture. One exit, so every route cleans up the same way.
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }
            .collect { if (opened && it == SheetAnchor.HIDDEN) entry.onDismissRequest() }
    }

    // The back gesture shrinks and fades the sheet under the thumb and lets go of it if the gesture
    // completes. Cancelled, it springs back — which is the whole point of the predictive version.
    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler { events ->
        try {
            events.collect { backProgress.snapTo(it.progress) }
            backProgress.snapTo(0f)
            state.animateTo(SheetAnchor.HIDDEN)
        } catch (cancelled: CancellationException) {
            backProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
            throw cancelled
        }
    }

    val offset = if (state.anchors.size > 0) state.requireOffset() else sheetHeight.toFloat()
    val shown = if (sheetHeight > 0) (1f - offset / sheetHeight).coerceIn(0f, 1f) else 0f

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerHeight = it.height }
            .background(Color.Black.copy(alpha = SCRIM_ALPHA * shown * (1f - backProgress.value)))
            // Tapping away is a dismissal, and it is the sheet that animates out rather than the
            // whole layer blinking off. The scrim also swallows the touches the page behind must not
            // receive while a modal sheet is open.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { scope.launch { state.animateTo(SheetAnchor.HIDDEN) } },
            ),
    )

    val sheetShape = SquircleTopShape(MobileDimens.SheetCorner)
    val settle = remember { Animatable(if (animations == AnimationLevel.OFF) 0f else SETTLE_DP) }
    LaunchedEffect(Unit) {
        if (animations != AnimationLevel.OFF) settle.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
    }
    val body = entry.content

    Column(
        entry.modifier
            .align(Alignment.BottomCenter)
            .widthIn(max = SheetMaxWidth)
            .fillMaxWidth()
            .imePadding()
            .onSizeChanged { sheetHeight = it.height }
            .offset { IntOffset(0, offset.roundToInt()) }
            .graphicsLayer {
                val shrink = lerp(1f, BACK_SCALE, backProgress.value)
                scaleX = shrink
                scaleY = shrink
                alpha = 1f - backProgress.value * 0.4f
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .nestedScroll(rememberSheetNestedScroll(state))
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Vertical,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
            )
            .semantics {
                isTraversalGroup = true
                entry.title?.let { paneTitle = it }
            }
            .glassSurface(GlassSurface.DIALOGS, sheetShape),
    ) {
        BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
        // The settle rides an inner layer, so what catches up is what is inside the pane — put it on
        // the pane itself and the glass slides off its own bottom edge.
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = settle.value * density },
        ) {
            val column = this
            entry.title?.let {
                Text(
                    text = it,
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
            // Everything in a sheet is already standing on a pane of glass, so it draws as the layer
            // behind one — no second frost over the first.
            GlassNest(GlassSurface.DIALOGS) { column.body() }
            // The gesture bar sits over the sheet's last row otherwise.
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .padding(bottom = MobileDimens.GapSmall),
            )
        }
    }
}

/**
 * Hand the drag between the sheet and the list inside it.
 *
 * Dragging up, the sheet expands before the list starts scrolling; dragging down, the list scrolls
 * until it reaches its own top and only then does the sheet take over. Without this a list inside a
 * sheet swallows every gesture and the sheet can only be moved by its handle.
 */
@Composable
private fun rememberSheetNestedScroll(
    state: AnchoredDraggableState<SheetAnchor>,
): NestedScrollConnection = remember(state) {
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f && source == NestedScrollSource.UserInput) {
                Offset(0f, state.dispatchRawDelta(available.y))
            } else {
                Offset.Zero
            }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (source == NestedScrollSource.UserInput) {
                Offset(0f, state.dispatchRawDelta(available.y))
            } else {
                Offset.Zero
            }

        override suspend fun onPreFling(available: Velocity): Velocity =
            if (available.y < 0f && state.requireOffset() > state.anchors.minPosition()) {
                state.settle(available.y)
                available
            } else {
                Velocity.Zero
            }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            state.settle(available.y)
            return available
        }
    }
}
