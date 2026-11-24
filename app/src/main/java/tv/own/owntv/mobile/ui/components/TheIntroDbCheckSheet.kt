package tv.own.owntv.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject
import tv.own.owntv.core.database.entity.EpisodeEntity
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.database.entity.SeriesEntity
import tv.own.owntv.core.skip.SkipKind
import tv.own.owntv.core.skip.TheIntroDbCheck
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.screens.settings.SheetText
import tv.own.owntv.mobile.ui.stage.StageMenuItem
import tv.own.owntv.mobile.ui.stage.StageSheetHeader

/**
 * "Check TheIntroDB" from a long-press menu: the menu posts the title here and closes, and the one
 * host in the activity asks TheIntroDB and shows what it has — so the film, series and episode menus
 * need no sheet state of their own.
 */
object TheIntroDbCheckRequests {
    sealed interface Request {
        data class Movie(val movie: MovieEntity) : Request
        data class Series(val show: SeriesEntity, val episode: EpisodeEntity?) : Request
    }

    internal val pending = MutableStateFlow<Request?>(null)

    fun movie(movie: MovieEntity) { pending.value = Request.Movie(movie) }
    fun series(show: SeriesEntity, episode: EpisodeEntity? = null) { pending.value = Request.Series(show, episode) }
}

@Composable
fun TheIntroDbCheckHost() {
    val request by TheIntroDbCheckRequests.pending.collectAsState()
    val req = request ?: return
    val check = koinInject<TheIntroDbCheck>()
    var result by remember(req) { mutableStateOf<TheIntroDbCheck.Result?>(null) }
    LaunchedEffect(req) {
        result = when (req) {
            is TheIntroDbCheckRequests.Request.Movie -> check.movie(req.movie)
            is TheIntroDbCheckRequests.Request.Series -> check.series(req.show, req.episode)
        }
    }
    val close = { TheIntroDbCheckRequests.pending.value = null }
    MobileBottomSheet(onDismissRequest = close) {
        StageSheetHeader(stringResource(R.string.settings_theintrodb))
        SheetText(result?.let { theIntroDbCheckText(it) } ?: "…")
        StageMenuItem(stringResource(R.string.common_ok), onClick = close, icon = MobileIcons.Check, filled = true)
    }
}

/** The title, its TMDB id (and episode), then what TheIntroDB has — and how to add what it lacks. */
@Composable
private fun theIntroDbCheckText(r: TheIntroDbCheck.Result): String {
    val sep = stringResource(R.string.content_metadata_separator)
    val answer = when (r) {
        is TheIntroDbCheck.Result.NoTmdb -> return r.title + "\n\n" + stringResource(R.string.theintrodb_check_no_tmdb)
        is TheIntroDbCheck.Result.Failed -> return r.title + "\n\n" + stringResource(R.string.settings_theintrodb_unreachable)
        is TheIntroDbCheck.Result.Answered -> r
    }
    val head = buildList {
        add(answer.title)
        add(stringResource(R.string.theintrodb_check_tmdb_id, answer.tmdbId))
        val season = answer.season
        val episode = answer.episode
        if (season != null && episode != null) add(stringResource(R.string.content_season_episode, season, episode))
    }.joinToString(sep)
    val partNames = answer.kinds.map { stringResource(partName(it)) }
    val line = when {
        partNames.isNotEmpty() -> stringResource(R.string.theintrodb_check_found, partNames.joinToString(sep))
        answer.listed -> stringResource(R.string.theintrodb_check_none)
        else -> stringResource(R.string.theintrodb_check_unlisted)
    }
    val contribute = if (partNames.isEmpty()) "\n\n" + stringResource(R.string.settings_theintrodb_contribute) else ""
    return head + "\n\n" + line + contribute
}

private fun partName(kind: SkipKind): Int = when (kind) {
    SkipKind.INTRO -> R.string.settings_skip_intro
    SkipKind.RECAP -> R.string.settings_skip_recap
    SkipKind.CREDITS -> R.string.settings_skip_credits
    SkipKind.PREVIEW -> R.string.settings_skip_preview
}
