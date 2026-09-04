package tv.own.owntv.mobile.ui.screens.settings

import androidx.annotation.StringRes
import tv.own.owntv.mobile.R

/**
 * A screen-sized setting that hangs off a group page — the third level of the tree.
 *
 * The television has nineteen screens under its settings root, several of them features in their own
 * right rather than a switch: Customize, the Glass Effect, the video player, Backup. A flat page
 * cannot hold one, so each becomes a route of its own here, registered once with its group, its
 * title and the keywords search matches on. Route, breadcrumb, page title and the search index all
 * read this list, so a leaf can never be reachable by one and invisible to another.
 *
 * A leaf is registered when its screen exists. The remaining television screens — Glass Effect,
 * Weather, Fonts, the navigation bar, Home, the long-press menus, DNS, Language and About — are still
 * rows on their group pages, and each moves here in the phase that builds it out; registering an
 * empty one now would put a dead row above content that already works.
 */
enum class SettingsLeaf(
    val group: SettingsGroup,
    slug: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val keywordsRes: Int,
    @param:StringRes val summaryRes: Int? = null,
) {
    PLAYLISTS(
        SettingsGroup.SOURCES, "playlists",
        R.string.settings_playlists, R.string.settings_search_keywords_playlists,
        R.string.settings_sources_description,
    ),
    EPG_SOURCES(
        SettingsGroup.SOURCES, "epg",
        R.string.settings_epg_sources, R.string.settings_search_keywords_epg,
        R.string.settings_epg_sources_description,
    ),

    CUSTOMIZE(
        SettingsGroup.CONTENT, "customize",
        R.string.settings_customize_title, R.string.settings_search_keywords_customize,
        R.string.settings_customize_nav_description,
    ),
    METADATA(
        SettingsGroup.CONTENT, "metadata",
        R.string.settings_metadata, R.string.settings_search_keywords_metadata,
        R.string.settings_metadata_source_description,
    ),
    OPEN_SUBTITLES(
        SettingsGroup.CONTENT, "opensubtitles",
        R.string.settings_open_subtitles, R.string.settings_search_keywords_subtitle_appearance,
        R.string.settings_open_subtitles_access_priority,
    ),

    VIDEO_PLAYER(
        SettingsGroup.PLAYBACK, "video",
        R.string.settings_video_player, R.string.settings_search_keywords_video,
        R.string.settings_vp_section_engine_summary,
    ),
    SUBTITLE_APPEARANCE(
        SettingsGroup.PLAYBACK, "subtitles",
        R.string.settings_subtitle_appearance, R.string.settings_search_keywords_subtitle_appearance,
        R.string.settings_vp_section_subtitles_summary,
    ),

    ERROR_LOG(
        SettingsGroup.APP, "errorlog",
        R.string.settings_playback_error_log, R.string.settings_search_keywords_errors,
    ),
    ;

    /** `settings/content/customize` — the group's own route with the leaf hung off it. */
    val route: String = "${group.route}/$slug"
}

/** The leaf a route names, or null when the route is a group page or not settings at all. */
fun settingsLeafOf(route: String?): SettingsLeaf? =
    SettingsLeaf.entries.firstOrNull { it.route == route }

/** The leaves of one group, in declaration order — what a group page lists at its head. */
fun leavesOf(group: SettingsGroup): List<SettingsLeaf> =
    SettingsLeaf.entries.filter { it.group == group }
