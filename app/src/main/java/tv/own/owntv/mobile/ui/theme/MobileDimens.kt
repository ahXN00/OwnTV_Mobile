package tv.own.owntv.mobile.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and sizing for a screen held at arm's length, not across a room. The TV app's values
 * are roughly double these: 48 dp screen padding becomes 16 (there is no overscan to avoid),
 * 180 dp posters become 100 (three columns have to fit a 360 dp phone).
 */
object MobileDimens {
    /** Material's accessibility floor. Nothing tappable is ever smaller. */
    val TouchTarget = 48.dp

    val ScreenPaddingH = 16.dp
    val ScreenPaddingV = 12.dp

    val GapTiny = 4.dp
    val GapSmall = 8.dp
    val GapMedium = 16.dp
    val GapLarge = 24.dp

    val CardCorner = 12.dp
    val SheetCorner = 24.dp

    val PosterWidthPortrait = 100.dp
    val PosterWidthLandscape = 120.dp
    val PosterArtCorner = 10.dp
    val PosterProgressHeight = 3.dp

    /** Row height for a list of channels or settings — one line of title plus one of subtitle. */
    val ListRowHeight = 64.dp
    val ListRowIconSize = 40.dp
}
