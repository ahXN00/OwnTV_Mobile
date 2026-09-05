package tv.own.owntv.mobile.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.MobileNavShape
import tv.own.owntv.mobile.ui.theme.glassSurface
import tv.own.owntv.player.OwnTVPlayer

private val BAR_HEIGHT = 64.dp
private const val DISMISS_DRAG_PX = 120f

/**
 * What is playing, kept in reach while the user goes looking for something else.
 *
 * It is the same engine and the same stream as the full screen player — the picture simply moves into
 * a smaller box — so opening it costs nothing and closing it is the only thing in the app that
 * actually stops playback. A radio channel has no picture to move, so its artwork stands in.
 *
 * It is told what to say rather than which tuner to ask, because a channel and a film are two
 * different objects and this bar is the same bar for both.
 */
@Composable
fun MiniPlayer(
    player: OwnTVPlayer,
    title: String,
    onExpand: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artworkUrl: String? = null,
) {
    val playing by player.isPlaying.collectAsStateWithLifecycle()
    val position by player.position.collectAsStateWithLifecycle()
    val duration by player.duration.collectAsStateWithLifecycle()
    val audioOnly by player.audioOnlyMedia.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxWidth()
            // An island above the navigation island, matching its corner: docked no longer means
            // welded to the bottom edge.
            .glassSurface(GlassSurface.MINI_PLAYER, MobileNavShape)
            .clip(MobileNavShape)
            .clickable(onClick = onExpand)
            .pointerInput(Unit) {
                var travel = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (travel > DISMISS_DRAG_PX) onStop()
                        travel = 0f
                    },
                    onVerticalDrag = { _, delta -> travel += delta },
                )
            },
    ) {
        Row(
            Modifier.height(BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (audioOnly) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxHeight(),
                    )
                } else {
                    VideoStage(player = player, modifier = Modifier.fillMaxHeight())
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = MobileDimens.GapSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = { player.togglePlayPause() }) {
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.settings_remote_action_play_pause),
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Close, stringResource(R.string.content_close))
            }
        }
        // Live has no end to move towards, so the line only appears for something that does.
        if (duration > 0) {
            LinearProgressIndicator(
                progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
