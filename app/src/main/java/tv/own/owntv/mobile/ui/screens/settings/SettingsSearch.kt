package tv.own.owntv.mobile.ui.screens.settings

import androidx.annotation.StringRes
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
    val title: String,
    private val keywords: String,
) {
    val haystack: String = "$title $keywords".lowercase()
}

/** Every settings row worth finding by name, grouped by the page it lives on. */
@Composable
fun rememberSettingsSearchEntries(): List<SettingsSearchEntry> {
    fun entries(): List<Triple<SettingsGroup, Int, Int>> = listOf(
        Triple(SettingsGroup.PROFILE, R.string.profiles_title, R.string.settings_search_keywords_profiles),

        Triple(SettingsGroup.SOURCES, R.string.settings_playlists, R.string.settings_search_keywords_playlists),
        Triple(SettingsGroup.SOURCES, R.string.settings_epg_sources, R.string.settings_search_keywords_epg),
        Triple(SettingsGroup.SOURCES, R.string.content_epg_time_offset, R.string.settings_search_keywords_epg_offset),
        Triple(SettingsGroup.SOURCES, R.string.settings_catchup, R.string.settings_search_keywords_catchup),
        Triple(SettingsGroup.SOURCES, R.string.settings_epg_sources_use_logos, R.string.settings_search_keywords_logos),

        Triple(SettingsGroup.APPEARANCE, R.string.settings_theme, R.string.settings_search_keywords_theme),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_accent, R.string.settings_search_keywords_accent),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_selection_highlight, R.string.settings_search_keywords_focus),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_glass_effect, R.string.settings_search_keywords_theme),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_ambient_glow, R.string.settings_search_keywords_theme),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_font_customization, R.string.settings_search_keywords_fonts),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_popup_size, R.string.settings_search_keywords_zoom),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_ui_zoom, R.string.settings_search_keywords_zoom),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_animations, R.string.settings_search_keywords_animation),
        Triple(SettingsGroup.APPEARANCE, R.string.settings_weather, R.string.settings_search_keywords_weather),

        Triple(SettingsGroup.LAYOUT, R.string.settings_nav_bar_customization, R.string.settings_search_keywords_sidebar),
        Triple(SettingsGroup.LAYOUT, R.string.settings_browsing_lists, R.string.settings_search_keywords_browsing),
        Triple(SettingsGroup.LAYOUT, R.string.settings_home_root, R.string.settings_search_keywords_home),
        Triple(SettingsGroup.LAYOUT, R.string.settings_content_menus_title, R.string.settings_search_keywords_browsing),
        Triple(SettingsGroup.LAYOUT, R.string.content_epg_title, R.string.settings_search_keywords_guide_width),

        Triple(SettingsGroup.CONTENT, R.string.settings_customize, R.string.settings_search_keywords_customize),
        Triple(SettingsGroup.CONTENT, R.string.settings_metadata, R.string.settings_search_keywords_metadata),
        Triple(SettingsGroup.CONTENT, R.string.settings_open_subtitles, R.string.settings_search_keywords_subtitle_appearance),

        Triple(SettingsGroup.PLAYBACK, R.string.settings_video_player, R.string.settings_search_keywords_video),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_quick_hdr, R.string.settings_search_keywords_hdr),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_surround_sound, R.string.settings_search_keywords_surround),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_quick_autoplay, R.string.settings_search_keywords_autoplay),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_live_latency, R.string.settings_search_keywords_latency),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_live_preroll, R.string.settings_search_keywords_live_preroll),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_subtitle_appearance, R.string.settings_search_keywords_subtitle_appearance),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_background_playback, R.string.settings_search_keywords_background_playback),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_pip, R.string.settings_search_keywords_pip),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_data_saver, R.string.settings_search_keywords_data_saver),
        Triple(SettingsGroup.PLAYBACK, R.string.settings_gesture_sensitivity, R.string.settings_search_keywords_gestures),

        Triple(SettingsGroup.NETWORK, R.string.common_proxy, R.string.settings_search_keywords_proxy),
        Triple(SettingsGroup.NETWORK, R.string.settings_dns, R.string.settings_search_keywords_dns),

        Triple(SettingsGroup.DATA, R.string.settings_backup_restore, R.string.settings_search_keywords_backup),
        Triple(SettingsGroup.DATA, R.string.settings_download_folder, R.string.settings_search_keywords_download),
        Triple(SettingsGroup.DATA, R.string.settings_downloads_wifi_only, R.string.settings_search_keywords_wifi_only),
        Triple(SettingsGroup.DATA, R.string.settings_clear_history, R.string.settings_search_keywords_history),

        Triple(SettingsGroup.APP, R.string.settings_language, R.string.settings_search_keywords_language),
        Triple(SettingsGroup.APP, R.string.settings_app_startup, R.string.settings_search_keywords_startup),
        Triple(SettingsGroup.APP, R.string.settings_about, R.string.settings_search_keywords_about),
        Triple(SettingsGroup.APP, R.string.settings_playback_error_log, R.string.settings_search_keywords_errors),
        Triple(SettingsGroup.APP, R.string.settings_diagnostics, R.string.settings_search_keywords_detailed_logging),
    )

    val resolved = entries().map { (group, title, keywords) ->
        SettingsSearchEntry(group, stringResource(title), stringResource(keywords))
    }
    return remember(resolved) { resolved }
}

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

/** The "Playback › Data saver" line under a search result. */
@Composable
fun settingsBreadcrumb(entry: SettingsSearchEntry, @StringRes format: Int = R.string.settings_breadcrumb): String =
    stringResource(format, stringResource(entry.group.titleRes), entry.title)
