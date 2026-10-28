package tv.own.owntv.mobile.ui.categorylock

import tv.own.owntv.core.categorylock.PlayingCategories
import tv.own.owntv.core.categorylock.SealedPlaybackGuard
import tv.own.owntv.core.categorylock.sealedTileIndices
import tv.own.owntv.mobile.ui.screens.library.VodTuner
import tv.own.owntv.mobile.ui.screens.live.LiveTuner

/**
 * The Multiview grid on screen, for the relock: the player screen sets [tiles] (each tile's channel
 * category) and [clear] (empty one tile) while the grid is up, and clears both when it goes.
 */
class OpenMultiview {
    var tiles: (() -> List<Long?>)? = null
    var clear: ((tile: Int) -> Unit)? = null
}

/**
 * What the relock stops on the phone when a category it closes is playing: the live channel (full
 * screen, in the preview or replaying its archive), the film, episode or recording, and each Multiview
 * tile on a channel of a sealed category.
 */
internal fun sealedPlaybackGuard(live: LiveTuner, vod: VodTuner, multiview: OpenMultiview): SealedPlaybackGuard =
    SealedPlaybackGuard(
        playing = {
            PlayingCategories(
                live = live.channel.value?.takeIf { live.hasStream || live.previewing.value }?.categoryId,
                liveInPlayer = live.hasStream,
                vod = vod.playing.value?.categoryId,
                tiles = multiview.tiles?.invoke().orEmpty(),
            )
        },
        stopLive = { _, _ -> live.stop() },
        stopVod = { vod.stop() },
        stopTiles = { sealed ->
            val tiles = multiview.tiles?.invoke().orEmpty()
            sealedTileIndices(tiles, sealed).forEach { multiview.clear?.invoke(it) }
        },
    )
