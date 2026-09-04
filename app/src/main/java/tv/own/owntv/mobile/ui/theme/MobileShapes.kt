package tv.own.owntv.mobile.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath

/**
 * A rounded rectangle whose corners have **continuous curvature**: the curve starts further along the
 * edge and eases into the turn instead of meeting the straight side at a sudden change of radius.
 *
 * It is what makes the Aurora glass read as a moulded pane rather than a rectangle with its corners
 * filed off, and the difference is most visible exactly where this app uses it — large sheets and
 * panels against a photograph. The maths is AndroidX's; a hand-rolled approximation goes visibly
 * wrong at small radii, which is the one place a corner is looked at closely.
 */
class SquircleShape(
    private val corner: Dp,
    // 0 is an ordinary rounded corner, 1 spreads the curve as far along the edge as it will go.
    private val smoothing: Float = 0.6f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { corner.toPx() }
            // A corner cannot be deeper than half the shorter side, and a squircle's curve reaches
            // further than its radius, so leave the smoothing its room too.
            .coerceAtMost(minOf(size.width, size.height) / 2f / (1f + smoothing))
        val polygon = RoundedPolygon.rectangle(
            width = size.width,
            height = size.height,
            rounding = CornerRounding(radius = radius, smoothing = smoothing),
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )
        return Outline.Generic(polygon.toPath().asComposePath())
    }
}

/**
 * The same shape with the top two corners curved and the bottom two square — a bottom sheet, which
 * only ever shows its top edge.
 */
class SquircleTopShape(private val corner: Dp, private val smoothing: Float = 0.6f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val overhang = with(density) { corner.toPx() } * (1f + smoothing) * 2f
        // The full four-cornered shape, grown past the bottom edge and then cut back to the sheet's
        // own bounds: the bottom pair of corners is cut away square and the top pair survives whole.
        val grown = SquircleShape(corner, smoothing)
            .createOutline(Size(size.width, size.height + overhang), layoutDirection, density)
        // Into a fresh path: an intersect that reads and writes the same path is undefined, and it
        // comes back empty — which clips the whole sheet away rather than squaring off its bottom.
        val clipped = Path()
        clipped.op(
            path1 = (grown as Outline.Generic).path,
            path2 = Path().apply { addRect(Rect(Offset.Zero, size)) },
            operation = PathOperation.Intersect,
        )
        return Outline.Generic(clipped)
    }
}

/** The shape of every card, poster and panel in the app. */
val MobileCardShape: Shape = SquircleShape(MobileDimens.CardCorner)
