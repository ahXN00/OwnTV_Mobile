package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import tv.own.owntv.mobile.R

/**
 * One searchable setting. The keyword strings are core's own — the same ones the TV app's settings
 * search matches on — so "cellular", "roaming" or "अंधेरा" find the row in whatever language the app
 * is running in, without a second list to keep in step.
 */
data class SettingsSearchEntry(
    val group: SettingsGroup,
    val leaf: SettingsLeaf?,
    val title: String,
    private val keywords: String,
) {
    val haystack: String = "$title $keywords".lowercase()

    /** Where tapping the result lands — the leaf that holds the row, or the group page. */
    val route: String = leaf?.route ?: group.route
}

/**
 * Every settings row worth finding by name.
 *
 * The leaves come from the registry rather than being listed twice, so a leaf added there is
 * searchable the moment it exists. Below them are the rows that sit directly on a group page, and
 * the rows deep inside a leaf that a user would still search for by name.
 */
@Composable
fun rememberSettingsSearchEntries(): List<SettingsSearchEntry> {
    // group, leaf (null when the row is on the group page itself), title, keywords
    fun rows(): List<SettingsRowEntry> = listOf(
        SettingsRowEntry(SettingsGroup.SOURCES, null, R.string.content_epg_time_offset, R.string.settings_search_keywords_epg_offset),
        SettingsRowEntry(SettingsGroup.SOURCES, null, R.string.settings_catchup, R.string.settings_search_keywords_catchup),
        SettingsRowEntry(SettingsGroup.SOURCES, null, R.string.settings_playlists, R.string.settings_search_keywords_playlists),
        SettingsRowEntry(SettingsGroup.SOURCES, null, R.string.settings_epg_sources, R.string.settings_search_keywords_epg),
        SettingsRowEntry(SettingsGroup.SOURCES, null, R.string.settings_epg_sources_use_logos, R.string.settings_search_keywords_logos),

        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_theme, R.string.settings_search_keywords_theme),
        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_accent, R.string.settings_search_keywords_accent),
        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_selection_highlight, R.string.settings_search_keywords_focus),
        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_ambient_glow, R.string.settings_search_keywords_theme),
        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_popup_size, R.string.settings_search_keywords_zoom),
        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_ui_zoom, R.string.settings_search_keywords_zoom),
        SettingsRowEntry(SettingsGroup.APPEARANCE, null, R.string.settings_animations, R.string.settings_search_keywords_animation),

        SettingsRowEntry(SettingsGroup.LAYOUT, null, R.string.settings_browsing_lists, R.string.settings_search_keywords_browsing),

        SettingsRowEntry(SettingsGroup.PLAYBACK, SettingsLeaf.VIDEO_PLAYER, R.string.settings_quick_hdr, R.string.settings_search_keywords_hdr),
        SettingsRowEntry(SettingsGroup.PLAYBACK, SettingsLeaf.VIDEO_PLAYER, R.string.settings_surround_sound, R.string.settings_search_keywords_surround),
        SettingsRowEntry(SettingsGroup.PLAYBACK, SettingsLeaf.VIDEO_PLAYER, R.string.settings_quick_autoplay, R.string.settings_search_keywords_autoplay),
        SettingsRowEntry(SettingsGroup.PLAYBACK, SettingsLeaf.VIDEO_PLAYER, R.string.settings_live_latency, R.string.settings_search_keywords_latency),
        SettingsRowEntry(SettingsGroup.PLAYBACK, SettingsLeaf.VIDEO_PLAYER, R.string.settings_live_preroll, R.string.settings_search_keywords_live_preroll),
        SettingsRowEntry(SettingsGroup.PLAYBACK, null, R.string.settings_background_playback, R.string.settings_search_keywords_background_playback),
        SettingsRowEntry(SettingsGroup.PLAYBACK, null, R.string.settings_pip, R.string.settings_search_keywords_pip),
        SettingsRowEntry(SettingsGroup.PLAYBACK, null, R.string.settings_data_saver, R.string.settings_search_keywords_data_saver),
        SettingsRowEntry(SettingsGroup.PLAYBACK, null, R.string.settings_gesture_sensitivity, R.string.settings_search_keywords_gestures),

        SettingsRowEntry(SettingsGroup.NETWORK, null, R.string.common_proxy, R.string.settings_search_keywords_proxy),

        SettingsRowEntry(SettingsGroup.DATA, null, R.string.settings_download_folder, R.string.settings_search_keywords_download),
        SettingsRowEntry(SettingsGroup.DATA, null, R.string.settings_downloads_wifi_only, R.string.settings_search_keywords_wifi_only),
        SettingsRowEntry(SettingsGroup.DATA, null, R.string.settings_clear_history, R.string.settings_search_keywords_history),

        SettingsRowEntry(SettingsGroup.APP, null, R.string.settings_app_startup, R.string.settings_search_keywords_startup),
        SettingsRowEntry(SettingsGroup.APP, null, R.string.settings_diagnostics, R.string.settings_search_keywords_detailed_logging),
    )

    val leaves = SettingsLeaf.entries.map {
        SettingsSearchEntry(it.group, it, stringResource(it.titleRes), stringResource(it.keywordsRes))
    }
    val resolved = leaves + rows().map {
        SettingsSearchEntry(
            it.group,
            it.leaf,
            stringResource(it.titleRes),
            stringResource(it.keywordsRes),
        )
    }
    return remember(resolved) { resolved }
}

/** A row's registration, before its strings are resolved. */
private class SettingsRowEntry(
    val group: SettingsGroup,
    val leaf: SettingsLeaf?,
    val titleRes: Int,
    val keywordsRes: Int,
)

/**
 * Every word must appear somewhere in the row, in any order — "wifi download" finds "Download over
 * Wi-Fi only" and so does "download wifi". A blank query matches nothing, because the root already
 * shows everything.
 */
fun List<SettingsSearchEntry>.matching(query: String): List<SettingsSearchEntry> {
    val tokens = query.lowercase().split(' ').filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyList()
    return filter { entry -> tokens.all { entry.haystack.contains(it) } }
}

/**
 * "Playback › Video player › Live latency" — three levels when the row is inside a leaf, two when it
 * sits on the group page. Built by nesting the same two-part separator string rather than adding a
 * three-part one, so the separator can never differ between the halves.
 */
@Composable
fun settingsBreadcrumb(entry: SettingsSearchEntry): String {
    val group = stringResource(entry.group.titleRes)
    val leaf = entry.leaf ?: return stringResource(R.string.settings_breadcrumb, group, entry.title)
    if (leaf.titleRes == entry.group.titleRes) {
        return stringResource(R.string.settings_breadcrumb, group, entry.title)
    }
    val leafTitle = stringResource(leaf.titleRes)
    // A leaf's own result is already "Group › Leaf"; only a row inside one needs the third level.
    if (leafTitle == entry.title) {
        return stringResource(R.string.settings_breadcrumb, group, leafTitle)
    }
    val parent = stringResource(R.string.settings_breadcrumb, group, leafTitle)
    return stringResource(R.string.settings_breadcrumb, parent, entry.title)
}
