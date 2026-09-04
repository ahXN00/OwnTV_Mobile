package tv.own.owntv.mobile.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.own.owntv.core.theme.GlassConfig
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.ui.theme.LocalAccentOnVideo
import tv.own.owntv.mobile.ui.theme.LocalAnimations
import tv.own.owntv.mobile.ui.theme.LocalGlass
import tv.own.owntv.mobile.ui.theme.SquircleShape
import tv.own.owntv.mobile.ui.theme.luminousEdge
import tv.own.owntv.mobile.ui.theme.luminousRim
import tv.own.owntv.mobile.ui.theme.materialFor

/**
 * The player's own material.
 *
 * Everything here is drawn on the picture, which is why none of it uses the page's colours: the
 * theme's surfaces and its accent are chosen to sit on a page, and over a film there is no page. The
 * dock is black glass with a white hairline, the text is white, and the one accent used is
 * [LocalAccentOnVideo].
 *
 * What the picture costs us: the video arrives on a `SurfaceView`, and Compose can neither sample it
 * nor blur it — putting anything over it also drops it off the hardware overlay path. So the dock
 * cannot frost the *video*. Scoping *Player controls* into the Glass Effect gives it the Chrome
 * material's light instead — the moulded rim, the top sheen, the far edge and the cast shadow — over
 * the same black fill.
 */
private val DockFill = Color.Black.copy(alpha = 0.55f)
private val DockRim = Color.White.copy(alpha = 0.10f)

internal val DockShape: Shape = SquircleShape(22.dp)
internal val CapsuleShape: Shape = SquircleShape(44.dp)
private val ButtonShape: Shape = SquircleShape(14.dp)

/** Idle white: bright enough to read on a bright scene, quiet enough not to fight the picture. */
internal val OnVideo = Color.White.copy(alpha = 0.78f)

private const val EXPAND_MS = 140
private const val HOLD_MS = 400L
private const val LABEL_MAX_DP = 132

/** One pane of the player's chrome: black glass, hairline rim, and the material's light when scoped. */
@Composable
private fun Modifier.playerPane(
    shape: Shape,
    surface: GlassSurface = GlassSurface.PLAYER_CONTROLS,
): Modifier {
    val glass = LocalGlass.current
    if (!glass.isGlassy(surface)) {
        return this.background(DockFill, shape).border(1.dp, DockRim, shape)
    }
    // 0.55 is the shipped baseline for the light control and therefore renders at 1x, as on the TV.
    val light = (glass.highlightStrength / GlassConfig.DEFAULT_HIGHLIGHT_STRENGTH).coerceIn(0f, 1.8f)
    val material = materialFor(surface)
    return this
        .then(if (glass.depthEffects) Modifier.shadow(material.shadow, shape) else Modifier)
        .background(DockFill, shape)
        .drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            val stroke = 1.dp.toPx()
            val sheen = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f * material.sheen * light), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.45f,
            )
            val rim = luminousRim(size, material.rim * light, Color.White)
            val edge = luminousEdge(size, material.edge * light)
            onDrawBehind {
                drawPath(path, sheen)
                drawPath(path, edge, style = Stroke(width = stroke))
                drawPath(path, rim, style = Stroke(width = stroke))
            }
        }
        .border(1.dp, DockRim, shape)
}

/** The bottom container: the instrument over the tools, in one pane. */
@Composable
internal fun PlayerDock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(DockShape)
            .playerPane(DockShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

/** The transport buttons in one capsule, rather than floating loose over the picture. */
@Composable
internal fun TransportCapsule(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .clip(CapsuleShape)
            .playerPane(CapsuleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/**
 * The label a control grows into.
 *
 * On the television the trigger is focus. A finger has none, so here it is press-and-hold: hold a
 * button to be told what it is, tap it to use it. The two or three tools with no gesture twin are
 * *pinned* and carry their label all the time.
 */
@Composable
private fun ExpandingLabel(visible: Boolean, label: String, tint: Color) {
    val ms = LocalAnimations.current.scale(EXPAND_MS)
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(tween(ms)) + fadeIn(tween(ms)),
        exit = shrinkHorizontally(tween(ms)) + fadeOut(tween(ms)),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp).widthIn(max = LABEL_MAX_DP.dp),
        )
    }
}

/**
 * Hold to read, tap to act.
 *
 * The label is never *only* held behind a gesture: it is also the button's accessibility name, it is
 * a semantic click action a screen reader can perform outright, and the pinned tools show it.
 *
 * A raw pointer loop has no semantics of its own — that is the trap here. `IconButton` gave the old
 * tool bar its button role and its activation for free; this has to declare both.
 */
@Composable
private fun Modifier.holdToReveal(
    label: String,
    onExpanded: (Boolean) -> Unit,
    onClick: () -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    return this.semantics {
        contentDescription = label
        role = Role.Button
        onClick(label) { onClick(); true }
    }.pointerInput(onClick) {
        detectTapGestures(
            onPress = {
                val reveal = scope.launch {
                    delay(HOLD_MS)
                    onExpanded(true)
                }
                tryAwaitRelease()
                reveal.cancel()
                onExpanded(false)
            },
            // Present so a hold does not also count as a tap: holding asks what the button is, and
            // being answered by the button firing is the opposite of an answer.
            onLongPress = { },
            onTap = { onClick() },
        )
    }
}

/** A tool: a 48 dp square that grows sideways into its label. */
@Composable
internal fun CtrlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    pinned: Boolean = false,
) {
    var held by remember { mutableStateOf(false) }
    val open = held || pinned
    val pad by animateDpAsState(
        targetValue = if (open) 12.dp else 0.dp,
        animationSpec = tween(LocalAnimations.current.scale(EXPAND_MS)),
        label = "toolPadding",
    )
    val tint = if (active) LocalAccentOnVideo.current else if (held) Color.White else OnVideo
    Row(
        modifier
            .height(48.dp)
            .widthIn(min = 48.dp)
            .clip(ButtonShape)
            .holdToReveal(label, onExpanded = { held = it }, onClick = onClick)
            .padding(horizontal = pad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        ExpandingLabel(open, label, tint)
    }
}

/** The playback rate, where the rate itself is the glyph — "1.5x" already says what the button does. */
@Composable
internal fun SpeedButton(rate: String, label: String, active: Boolean, onClick: () -> Unit) {
    TextControl(text = rate, label = label, active = active, icon = null, onClick = onClick)
}

/** MPV or EXO: which engine is playing, and one tap to swap it. */
@Composable
internal fun EngineToggle(engine: String, label: String, active: Boolean, icon: ImageVector, onClick: () -> Unit) {
    TextControl(text = engine, label = label, active = active, icon = icon, onClick = onClick)
}

@Composable
private fun TextControl(
    text: String,
    label: String,
    active: Boolean,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    var held by remember { mutableStateOf(false) }
    val tint = if (active) LocalAccentOnVideo.current else if (held) Color.White else OnVideo
    Row(
        Modifier
            .height(48.dp)
            .clip(ButtonShape)
            .holdToReveal(label, onExpanded = { held = it }, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.SemiBold)
        ExpandingLabel(held, label, tint)
    }
}

/** The channel's logo, falling back to the first letters of its name on the house plate. */
@Composable
internal fun ChannelLogo(logoUrl: String?, title: String?, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(
        modifier.size(size).clip(SquircleShape(10.dp)).background(Color(0xFF004F46)),
        contentAlignment = Alignment.Center,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(model = logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else if (!title.isNullOrBlank()) {
            Text(
                title.take(3).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6FF8E4),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * A message over the picture — the volume readout, the speed, a failure.
 *
 * Its own surface, so a user who wants the chrome frosted but the messages plain (or the other way
 * about) can have that: *On-screen messages* is a separate switch on the Glass Effect screen.
 */
@Composable
internal fun PlayerToast(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(ToastShape)
            .playerPane(ToastShape, GlassSurface.TOASTS)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

private val ToastShape: Shape = SquircleShape(18.dp)

/**
 * The scrims: a flat panel behind the chrome, feathered to nothing only at its inner edge.
 *
 * A plain two-stop gradient fades out exactly where the title and the times sit, so those wash out on
 * a bright scene; a hard band instead draws a seam across the picture. Panel first, then the feather.
 */
internal val TopScrim = Brush.verticalGradient(
    0.0f to Color.Black.copy(alpha = 0.72f),
    0.5f to Color.Black.copy(alpha = 0.68f),
    1.0f to Color.Transparent,
)

internal val BottomScrim = Brush.verticalGradient(
    0.0f to Color.Transparent,
    0.45f to Color.Black.copy(alpha = 0.68f),
    1.0f to Color.Black.copy(alpha = 0.78f),
)
