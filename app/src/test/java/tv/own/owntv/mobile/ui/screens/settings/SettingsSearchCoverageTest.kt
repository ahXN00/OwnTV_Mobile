package tv.own.owntv.mobile.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Settings search keeps a hand-written list of rows beside the real pages, and it had drifted: the
 * whole Video player page except six rows was unfindable by name — Multiview, both engine pickers,
 * the zoom, seek and volume steps, the language preferences — and so were custom DNS and the nav bar.
 *
 * Pages cannot drift, because they come from `SettingsLeaf.entries`. Rows can, and this is what holds
 * them: every row title drawn on the Video player page must appear in the search list.
 *
 * Source-scanned rather than executed — these are Compose pages needing a full Android graph, and the
 * thing worth checking is the text itself.
 */
class SettingsSearchCoverageTest {

    private fun read(name: String): String {
        val file = File("src/main/java/tv/own/owntv/mobile/ui/screens/settings/$name")
        assertTrue("expected to run from the app module, cwd=${File(".").absolutePath}", file.isFile)
        return file.readText()
    }

    private val search: String by lazy { read("SettingsSearch.kt") }

    /**
     * Titles search can match, by string resource name — the hand-listed rows *and* the leaf registry,
     * since a row that opens a whole page is already findable under that page's own name.
     */
    private val indexed: Set<String> by lazy {
        Regex("""R\.string\.([a-z_0-9]+)""")
            .findAll(search + read("SettingsLeaf.kt"))
            .map { it.groupValues[1] }
            .toSet()
    }

    /**
     * Rows on the page that are deliberately not indexed on their own: the picker labels and "Custom"
     * chips that are parts of a row rather than rows, and the three nav titles reused as section heads.
     */
    private val notRows = setOf(
        "common_nav_live_tv", "common_nav_movies", "common_nav_series",
        "settings_live_latency_custom", "settings_live_preroll_playlist_picker",
    )

    @Test
    fun `every video player row can be found by search`() {
        val page = read("SettingsPlaybackPage.kt")
        val titles = Regex("""title = stringResource\(R\.string\.([a-z_0-9]+)""")
            .findAll(page).map { it.groupValues[1] }
            .filterNot { it.endsWith("_description") || it.endsWith("_description_mobile") || it in notRows }
            .toSortedSet()
        assertTrue("no rows found on the Video player page — has the file changed shape?", titles.size > 20)
        assertEquals(
            "Video player rows missing from the settings search index — unfindable by name",
            emptyList<String>(),
            titles.filterNot { it in indexed }.sorted(),
        )
    }

    @Test
    fun `every settings page is indexed automatically`() {
        // The leaves are mapped from the registry rather than listed again; if that ever becomes a
        // hand-written list, pages start going missing the way the rows did.
        assertTrue(
            "SettingsSearch no longer derives its page entries from SettingsLeaf.entries",
            search.contains("SettingsLeaf.entries"),
        )
    }

    @Test
    fun `search entries name a real string resource`() {
        // A typo'd resource would not compile, but a row entry pointing at a *description* string reads
        // as a sentence in the results instead of a title.
        assertEquals(
            "search entries should use a row's title, not its description",
            emptyList<String>(),
            Regex("""SettingsRowEntry\([^)]*R\.string\.([a-z_0-9]*_description)""")
                .findAll(search).map { it.groupValues[1] }.toList(),
        )
    }
}
