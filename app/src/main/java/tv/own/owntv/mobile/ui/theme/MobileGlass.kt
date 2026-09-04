package tv.own.owntv.mobile.ui.theme

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.theme.GlassConfig
import tv.own.owntv.core.theme.GlassSurface

/** The user's Glass Effect settings, read from core and provided at the theme root. */
val LocalGlass = staticCompositionLocalOf { GlassConfig() }

/**
 * The wallpaper, blurred once, that every glass surface samples its own slice of.
 *
 * One blur for the whole screen rather than one per panel: the layer is recorded a single time and
 * each surface merely draws it back, shifted so the slice lines up with what is actually behind it.
 * A phone with fifty glass list rows on screen therefore costs the same as one.
 */
class GlassBackdrop internal constructor(
    internal val layer: GraphicsLayer,
    internal var rootOffset: Offset,
)

/**
 * The wallpaper's own dominant colour, or null when there is no wallpaper.
 *
 * A pane of glass takes the colour of what is behind it. Tinting the panels a little way towards the
 * wallpaper is what stops a grey Material surface sitting on a warm photograph looking like a sticker
 * laid on top of it — and it costs one downsampled decode when the wallpaper changes, not per frame.
 */
val LocalGlassTint = staticCompositionLocalOf<Color?> { null }

/** How far a glass panel is pulled towards the wallpaper's colour. Enough to relate, not to stain. */
private const val TINT_MIX = 0.22f

/**
 * The fill of a glass panel that has no frost behind it — the TV app's own tonal value.
 *
 * Not 1.0: a hair of translucency is what still separates it from a plain Material surface. Not the
 * user's alpha either, because that number describes how much wallpaper shows through the frost, and
 * here there is no frost for it to describe.
 */
private const val CERAMIC_ALPHA = 0.94f

/** Averaged over a thumbnail — 32 px is plenty for "what colour is this picture, roughly". */
private suspend fun dominantColour(path: String): Color? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 32)
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return@runCatching null
        var r = 0L
        var g = 0L
        var b = 0L
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
            }
        }
        val n = (bitmap.width * bitmap.height).coerceAtLeast(1)
        bitmap.recycle()
        Color(red = (r / n).toInt(), green = (g / n).toInt(), blue = (b / n).toInt())
    }.getOrNull()
}

/** The blurred backdrop, or null when there is no wallpaper or the device predates hardware blur. */
val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

/**
 * Real backdrop blur is [android.graphics.RenderEffect], which is API 31. Below that the glass is
 * translucency, rim light and depth only — never a per-frame software blur, which on a phone would
 * cost battery on every scroll for an effect nobody asked to pay for.
 */
val supportsBackdropBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** How deep the frost goes at full strength. */
private val MaxBlurRadius = 44.dp

/**
 * The root the whole app sits in: the wallpaper sharp underneath, and a blurred copy of it kept
 * aside for [glassSurface] to sample.
 *
 * With no wallpaper set, or on a device without hardware blur, this is just the theme background and
 * the backdrop local stays null.
 */
@Composable
fun GlassBackdropRoot(content: @Composable () -> Unit) {
    val settings: SettingsRepository = koinInject()
    val bgImagePath by settings.bgImagePath.collectAsStateWithLifecycle("")
    val glass = LocalGlass.current
    val hasImage = bgImagePath.isNotBlank()
    val frosted = hasImage && supportsBackdropBlur && glass.enabled && glass.blurStrength > 0f

    // Recorded sharp, drawn sharp behind the content; the blurred layer replays it through the blur
    // so the wallpaper is decoded and rasterized exactly once for both.
    val sharp = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    val radiusPx = with(LocalDensity.current) {
        (MaxBlurRadius * glass.blurStrength).toPx().coerceAtLeast(0.1f)
    }
    val tint by produceState<Color?>(null, bgImagePath) {
        value = if (hasImage) dominantColour(bgImagePath) else null
    }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val backdrop = remember(frosted) { if (frosted) GlassBackdrop(blurred, Offset.Zero) else null }
    backdrop?.rootOffset = rootOffset

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { rootOffset = it.positionInRoot() },
    ) {
        if (hasImage) {
            AsyncImage(
                model = bgImagePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        sharp.record { this@drawWithContent.drawContent() }
                        drawLayer(sharp)
                        if (frosted) {
                            blurred.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Decal)
                            blurred.record { drawLayer(sharp) }
                        }
                    },
            )
        }
        CompositionLocalProvider(
            LocalGlassBackdrop provides backdrop,
            LocalGlassTint provides tint,
        ) {
            content()
        }
    }
}

/**
 * Fill a panel with its surface colour, translucent and edge-lit when the user has scoped this
 * [surface] into the Glass Effect. Off — which is the default — it is an ordinary opaque fill, so
 * every caller can use it unconditionally.
 *
 * Glassed, it draws in the order light actually arrives: the frosted slice of wallpaper behind the
 * panel, the tint at the user's own alpha over it, a specular rim around the edge and a highlight
 * along the top where a pane of glass catches the light.
 */
@Composable
fun Modifier.glassSurface(
    surface: GlassSurface,
    shape: Shape = MobileCardShape,
    // The colour this panel is when the effect is off. Passed where a panel already had a tone of
    // its own — the mini player and the bottom sheet — so turning glass off restores exactly it.
    fill: Color = MaterialTheme.colorScheme.surfaceContainer,
): Modifier {
    val glass = LocalGlass.current
    if (!glass.isGlassy(surface)) return background(fill, shape)

    val backdrop = LocalGlassBackdrop.current
    // Pulled towards the wallpaper's colour, so the panel looks lit by the same picture it sits on.
    val tinted = LocalGlassTint.current?.let { lerp(fill, it, TINT_MIX) } ?: fill
    val rim = MaterialTheme.colorScheme.onSurface
    // 0.55 is the shipped baseline and therefore renders at 1x, exactly as on the TV.
    val light = (glass.highlightStrength / GlassConfig.DEFAULT_HIGHLIGHT_STRENGTH).coerceIn(0f, 1.8f)
    // Text has to stay readable over a photograph. The floor is the one thing the alpha slider
    // cannot go below, unless the user has explicitly said it may.
    val tintAlpha = if (glass.allowFullTransparency) glass.alpha else glass.alpha.coerceAtLeast(0.22f)

    var position by remember { mutableStateOf(Offset.Zero) }
    return this
        // Depth is what stops a translucent panel reading as a stain on the wallpaper: it has to sit
        // above it, not in it. Off, the glass is flat and the wallpaper is the only depth cue.
        .then(if (glass.depthEffects) Modifier.shadow(6.dp, shape) else Modifier)
        .onGloballyPositioned { position = it.positionInRoot() }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            val topSheen = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.14f * light), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.45f,
            )
            onDrawBehind {
                clipPath(path) {
                    if (backdrop != null) {
                        val slice = position - backdrop.rootOffset
                        translate(-slice.x, -slice.y) { drawLayer(backdrop.layer) }
                        // The frosted slice is opaque underneath, so the tint is what the user set.
                        drawRect(tinted.copy(alpha = tintAlpha))
                    } else {
                        // Nothing frosted behind this panel to be translucent over — no wallpaper, a
                        // device without hardware blur, or a sheet, which is its own window and cannot
                        // replay the main window's layer. A 22 % fill over nothing is not glass, it is
                        // an invisible panel, so this is the TV app's tonal ceramic instead: near-opaque,
                        // wallpaper-tinted, and still edge-lit and sheened below.
                        drawRect(tinted.copy(alpha = CERAMIC_ALPHA))
                    }
                    drawRect(topSheen)
                }
                drawPath(
                    path = path,
                    color = rim.copy(alpha = 0.20f * light),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
}
