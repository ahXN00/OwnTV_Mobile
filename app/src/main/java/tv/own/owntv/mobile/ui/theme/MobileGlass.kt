package tv.own.owntv.mobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import tv.own.owntv.core.theme.GlassConfig
import tv.own.owntv.core.theme.GlassSurface

/** The user's Glass Effect settings, read from core and provided at the theme root. */
val LocalGlass = staticCompositionLocalOf { GlassConfig() }

/**
 * Fill a panel with its surface colour, translucent and edge-lit when the user has scoped this
 * [surface] into the Glass Effect. Off — which is the default — it is an ordinary opaque fill, so
 * every caller can use it unconditionally.
 *
 * The user's alpha choice is honoured exactly. The *frost* half of the effect is not here: real
 * backdrop blur needs something behind the panel to blur, and this app has no background layer
 * until the shell arrives.
 */
@Composable
fun Modifier.glassSurface(
    surface: GlassSurface,
    shape: Shape = RoundedCornerShape(MobileDimens.CardCorner),
): Modifier {
    val glass = LocalGlass.current
    val fill = MaterialTheme.colorScheme.surfaceContainer
    return if (!glass.isGlassy(surface)) {
        background(fill, shape)
    } else {
        background(fill.copy(alpha = glass.alpha), shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface
                    .copy(alpha = 0.20f * glass.highlightStrength),
                shape = shape,
            )
    }
}
