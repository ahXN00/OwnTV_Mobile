package tv.own.owntv.mobile.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import tv.own.owntv.core.nav.MainSection
import tv.own.owntv.mobile.R

/**
 * Where the phone can navigate to.
 *
 * [sections] is what ties a destination to core's visibility rule: a destination shows only when at
 * least one of its sections is in `NavVisibility.visibleSections()`. Library is one tab covering two
 * sections, which is why the rule is a set and not a single value — a movies-only playlist still
 * gets a Library tab, a channels-only one does not.
 *
 * MORE has no section: it is always reachable, because Settings, Profiles and Downloads live behind
 * it and a user must never be able to hide their way out of the app's own settings.
 */
enum class MobileDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val sections: Set<MainSection>,
) {
    HOME("home", R.string.common_nav_home, Icons.Filled.Home, setOf(MainSection.HOME)),
    LIVE("live", R.string.common_nav_live_tv, Icons.Filled.LiveTv, setOf(MainSection.LIVE_TV)),
    LIBRARY(
        "library",
        R.string.common_nav_library,
        Icons.Filled.VideoLibrary,
        setOf(MainSection.MOVIES, MainSection.SERIES),
    ),
    GUIDE("guide", R.string.common_nav_guide, Icons.Filled.CalendarMonth, setOf(MainSection.EPG)),
    MORE("more", R.string.common_nav_more, Icons.Filled.MoreHoriz, emptySet()),

    // Rail-only. On a phone these live inside Library and More; on a tablet there is room to show
    // them as destinations of their own, which is what step 3 of the plan calls the eight-item rail.
    MOVIES("movies", R.string.common_nav_movies, Icons.Filled.Movie, setOf(MainSection.MOVIES)),
    SERIES("series", R.string.common_nav_series, Icons.Filled.Theaters, setOf(MainSection.SERIES)),
    DOWNLOADS(
        "downloads",
        R.string.common_nav_downloads,
        Icons.Filled.Download,
        setOf(MainSection.DOWNLOADS),
    ),
    SETTINGS("settings", R.string.common_nav_settings, Icons.Filled.Settings, emptySet());

    companion object {
        /** The bottom bar, in order. Library stands in for Movies and Series on a narrow screen. */
        val bottomBar: List<MobileDestination> = listOf(HOME, LIVE, LIBRARY, GUIDE, MORE)

        /** The rail, in order: the six browse sections split apart, plus More and Settings. */
        val rail: List<MobileDestination> =
            listOf(HOME, LIVE, MOVIES, SERIES, GUIDE, DOWNLOADS, MORE, SETTINGS)

        /**
         * Filters a bar or rail to what the active playlist actually offers. A destination with no
         * sections is always kept; the rest survive if core says any of their sections is visible.
         */
        fun List<MobileDestination>.visible(sections: Set<MainSection>): List<MobileDestination> =
            filter { it.sections.isEmpty() || it.sections.any { section -> section in sections } }
    }
}
