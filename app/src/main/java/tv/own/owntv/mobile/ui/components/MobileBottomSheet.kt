package tv.own.owntv.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.theme.AnimationLevel
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.ui.theme.LocalGlass
import tv.own.owntv.mobile.ui.theme.LocalGlassBackdrop
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.SquircleTopShape
import tv.own.owntv.mobile.ui.theme.glassSurface

/** How far the content starts below where it ends up. Enough to read as weight, not as a slide. */
private const val SETTLE_DP = 18f

/** A sheet's list may take at most half the screen: the buttons under it have to stay reachable. */
@Composable
fun sheetListHeight() = (LocalConfiguration.current.screenHeightDp / 2).dp

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
    val sheetShape = SquircleTopShape(MobileDimens.SheetCorner)
    val glassy = LocalGlass.current.isGlassy(GlassSurface.DIALOGS)
    val settings: SettingsRepository = koinInject()
    val animations by settings.animationLevel.collectAsStateWithLifecycle(AnimationLevel.FULL)
    val animate = animations == AnimationLevel.FULL

    // A modal sheet is its own window, and the blurred backdrop belongs to the main one — its
    // coordinates mean nothing here and its layer cannot be replayed onto this canvas. So the sheet
    // wears the translucency and the rim without the frost, which is the same thing every device
    // below API 31 sees.
    CompositionLocalProvider(LocalGlassBackdrop provides null) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            // Never a background on this modifier: it wraps the sheet from outside the offset that
            // slides it up from the bottom, so anything painted here lands at the top of the screen
            // with the sheet's height. Glassed, the pane below is what draws the material; the
            // container gets out of its way. Not glassed, the container is the opaque Material one
            // this always used.
            modifier = modifier,
            shape = sheetShape,
            containerColor = if (glassy) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLow,
            // The handle belongs to the pane, not above it — otherwise the glass starts underneath it.
            dragHandle = null,
        ) {
            // The content settles a moment after the sheet has arrived, so the glass reads as
            // having mass — the pane stops and what is inside it catches up. Off entirely when the
            // user has turned animations off, which in this app means off, not softened.
            val settle = remember { Animatable(if (animate) SETTLE_DP else 0f) }
            LaunchedEffect(animate) {
                if (animate) settle.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
            }
            // fillMaxWidth, because this wrapper stands between the sheet and rows that used to be
            // the sheet's own children: without it the column takes its widest row's width and every
            // row is squeezed to the left of a full-width sheet.
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (glassy) {
                            Modifier.glassSurface(
                                surface = GlassSurface.DIALOGS,
                                shape = sheetShape,
                                fill = MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                BottomSheetDefaults.DragHandle(Modifier.align(Alignment.CenterHorizontally))
                // The settle rides an inner layer, so what catches up is what is inside the pane —
                // put it on the pane itself and the glass slides off its own bottom edge.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = settle.value * density },
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
                    Spacer(
                        Modifier
                            .navigationBarsPadding()
                            .padding(bottom = MobileDimens.GapSmall),
                    )
                }
            }
        }
    }
}
