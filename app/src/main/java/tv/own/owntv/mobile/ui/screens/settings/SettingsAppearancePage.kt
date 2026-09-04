package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.theme.AccentColor
import tv.own.owntv.core.theme.AnimationLevel
import tv.own.owntv.core.theme.AppFontFamily
import tv.own.owntv.core.theme.FontCustomization
import tv.own.owntv.core.theme.GlassConfig
import tv.own.owntv.core.theme.GlassPreset
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.core.theme.PopupFontScale
import tv.own.owntv.core.theme.PopupSizeScale
import tv.own.owntv.core.theme.ThemeMode
import tv.own.owntv.core.theme.UiFontScale
import tv.own.owntv.core.theme.UiZoom
import tv.own.owntv.core.theme.roles
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.SettingRow
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.theme.labelRes

/**
 * How the app looks. Everything on this page is applied by the theme the moment it is set, and the
 * panel at the top is a piece of the app drawn with the current values — the font, the accent and
 * the zoom are visible in it without leaving the page to check.
 */
@Composable
fun SettingsAppearancePage(
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = koinViewModel(),
) {
    val themeMode = vm.settings.themeMode.pref(ThemeMode.DARK)
    val accent = vm.settings.accent.pref(AccentColor.TEAL)
    val glass = vm.settings.glassConfig.pref(GlassConfig())
    val fonts = vm.settings.fontCustomization.pref(FontCustomization())
    val zoom = vm.settings.uiZoomPercent.pref(UiZoom.DEFAULT)
    val animations = vm.settings.animationLevel.pref(AnimationLevel.FULL)
    val weather = vm.settings.weatherEnabled.pref(false)
    val highlightWidth = vm.settings.focusHighlightWidth.pref(2)

    var sheet by remember { mutableStateOf<AppearanceSheet?>(null) }
    // The zoom the low-memory warning is holding, and whether its risk has already been accepted.
    var pendingLowZoom by remember { mutableStateOf<Int?>(null) }
    var lowZoomAccepted by remember { mutableStateOf(zoom < UiZoom.LOW_RAM_WARN) }

    SettingsPage(modifier) {
        item(key = "preview") { AppearancePreview() }

        item(key = "theme") {
            SettingRow(
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(R.string.settings_theme_description),
                value = stringResource(themeMode.labelRes()),
                onClick = { sheet = AppearanceSheet.THEME },
            )
        }
        item(key = "accent") {
            SettingRow(
                title = stringResource(R.string.settings_accent),
                subtitle = stringResource(R.string.settings_accent_description),
                value = stringResource(accent.labelRes),
                onClick = { sheet = AppearanceSheet.ACCENT },
            )
        }
        item(key = "highlight") {
            SettingRow(
                title = stringResource(R.string.settings_selection_highlight),
                subtitle = stringResource(R.string.settings_selection_highlight_description),
                value = focusWidthLabel(highlightWidth),
                onClick = { sheet = AppearanceSheet.HIGHLIGHT },
            )
        }
        item(key = "glass") {
            SettingRow(
                title = stringResource(R.string.settings_glass_effect),
                subtitle = stringResource(R.string.settings_glass_description),
                value = stringResource(
                    if (glass.enabled) R.string.common_on else R.string.common_off,
                ),
                onClick = { sheet = AppearanceSheet.GLASS },
            )
        }
        // The glow is a light behind solid panels, so it only means anything on a dark theme that is
        // not already showing a photograph through them — the same condition the TV app uses.
        if (themeMode == ThemeMode.DARK && !glass.enabled) {
            item(key = "glow") {
                SettingRow(
                    title = stringResource(R.string.settings_ambient_glow),
                    subtitle = stringResource(R.string.settings_ambient_glow_description),
                    checked = vm.settings.ambientGlowEnabled.pref(false),
                    onCheckedChange = { vm.edit { setAmbientGlowEnabled(it) } },
                )
            }
            item(key = "glow-pulse") {
                SettingRow(
                    title = stringResource(R.string.settings_ambient_glow_pulse),
                    checked = vm.settings.ambientGlowPulse.pref(false),
                    onCheckedChange = { vm.edit { setAmbientGlowPulse(it) } },
                )
            }
        }
        item(key = "fonts") {
            SettingRow(
                title = stringResource(R.string.settings_font_customization),
                subtitle = stringResource(R.string.settings_font_customization_description),
                value = stringResource(R.string.common_percent, fonts.sizePercent),
                onClick = { sheet = AppearanceSheet.FONTS },
            )
        }
        item(key = "popup-size") {
            SettingsSlider(
                title = stringResource(R.string.settings_popup_size),
                subtitle = stringResource(R.string.settings_popup_size_description),
                value = fonts.popupSizePercent,
                range = PopupSizeScale.MIN..PopupSizeScale.MAX,
                steps = stepsFor(PopupSizeScale.MIN, PopupSizeScale.MAX, PopupSizeScale.STEP),
                onValueChange = { pct ->
                    vm.edit { setFontCustomization(fonts.copy(popupSizePercent = PopupSizeScale.clamp(pct))) }
                },
            )
        }
        item(key = "zoom") {
            SettingsSlider(
                title = stringResource(R.string.settings_ui_zoom),
                subtitle = stringResource(R.string.settings_ui_zoom_description),
                value = zoom,
                range = UiZoom.MIN..UiZoom.MAX,
                steps = stepsFor(UiZoom.MIN, UiZoom.MAX, UiZoom.STEP),
                // Below the warning point a screen holds so many more items that a small-memory
                // device can run out of it. Ask once, on the drag that crosses the line, and
                // remember the answer for the rest of the visit rather than asking at every step.
                onValueChange = { raw ->
                    val pct = UiZoom.clamp(raw)
                    if (pct < UiZoom.LOW_RAM_WARN && !lowZoomAccepted) {
                        pendingLowZoom = pct
                    } else {
                        vm.edit { setUiZoomPercent(pct) }
                    }
                },
            )
        }
        item(key = "animations") {
            SettingRow(
                title = stringResource(R.string.settings_animations),
                subtitle = stringResource(R.string.settings_animations_description),
                checked = animations == AnimationLevel.FULL,
                onCheckedChange = { on ->
                    vm.edit { setAnimationLevel(if (on) AnimationLevel.FULL else AnimationLevel.OFF) }
                },
            )
        }

        settingsSection(R.string.settings_weather)
        settingsNote(R.string.settings_weather_description_root)
        item(key = "weather") {
            SettingRow(
                title = stringResource(R.string.settings_show_weather),
                subtitle = stringResource(R.string.settings_show_weather_description),
                checked = weather,
                onCheckedChange = { vm.edit { setWeatherEnabled(it) } },
            )
        }
        if (weather) {
            item(key = "weather-location") { WeatherLocationField(vm) }
            item(key = "weather-unit") {
                val fahrenheit = vm.settings.weatherFahrenheit.pref(false)
                SettingRow(
                    title = stringResource(R.string.settings_temperature_unit),
                    subtitle = stringResource(R.string.settings_temperature_description),
                    value = stringResource(
                        if (fahrenheit) R.string.settings_degree_fahrenheit
                        else R.string.settings_degree_celsius,
                    ),
                    onClick = { vm.edit { setWeatherFahrenheit(!fahrenheit) } },
                )
            }
        }
    }

    when (sheet) {
        AppearanceSheet.THEME -> SettingsChoiceSheet(
            title = stringResource(R.string.settings_theme),
            choices = ThemeMode.entries.map { SettingsChoice(it, stringResource(it.labelRes())) },
            selected = themeMode,
            onSelect = { mode -> vm.edit { setThemeMode(mode) } },
            onDismiss = { sheet = null },
        )
        AppearanceSheet.ACCENT -> SettingsChoiceSheet(
            title = stringResource(R.string.settings_accent),
            choices = AccentColor.entries.map { SettingsChoice(it, stringResource(it.labelRes)) },
            selected = accent,
            // A preset and a custom hex are two ways to answer the same question, so choosing a
            // preset clears the hex — otherwise the hex would keep winning and the taps do nothing.
            onSelect = { color -> vm.edit { setCustomAccent(""); setAccent(color) } },
            onDismiss = { sheet = null },
        )
        AppearanceSheet.HIGHLIGHT -> HighlightSheet(vm, onDismiss = { sheet = null })
        AppearanceSheet.GLASS -> GlassSheet(vm, glass, onDismiss = { sheet = null })
        AppearanceSheet.FONTS -> FontsSheet(vm, fonts, onDismiss = { sheet = null })
        null -> Unit
    }

    pendingLowZoom?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingLowZoom = null },
            title = { Text(stringResource(R.string.settings_low_zoom_warning_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_low_zoom_warning,
                        UiZoom.LOW_RAM_WARN,
                        UiZoom.LOW_RAM_WARN,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        lowZoomAccepted = true
                        pendingLowZoom = null
                        vm.edit { setUiZoomPercent(target) }
                    },
                ) { Text(stringResource(R.string.settings_low_zoom_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingLowZoom = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private enum class AppearanceSheet { THEME, ACCENT, HIGHLIGHT, GLASS, FONTS }

/** A slider's stops, so it lands on core's step size instead of anywhere between two of them. */
private fun stepsFor(min: Int, max: Int, step: Int): Int = ((max - min) / step) - 1

/** The ring's width as a word, the way the TV app names it: Thin, Normal, Thick, Extra thick. */
@Composable
private fun focusWidthLabel(dp: Int): String = stringResource(
    when (dp) {
        1 -> R.string.settings_focus_width_thin
        4, 5 -> R.string.settings_focus_width_thick
        6 -> R.string.settings_focus_width_extra
        else -> R.string.settings_focus_width_normal
    },
)

@Composable
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.DARK -> R.string.settings_theme_dark
    ThemeMode.LIGHT -> R.string.settings_theme_light
}

@Composable
private fun AppFontFamily.labelRes(): Int = when (this) {
    AppFontFamily.LORA -> R.string.settings_font_lora
    AppFontFamily.SYSTEM_SANS -> R.string.settings_font_system_sans
    AppFontFamily.MONOSPACE -> R.string.settings_font_monospace
    AppFontFamily.PLAYFAIR_DISPLAY -> R.string.settings_font_playfair_display
    AppFontFamily.DANCING_SCRIPT -> R.string.settings_font_dancing_script
    AppFontFamily.POPPINS -> R.string.settings_font_poppins
}

@Composable
private fun GlassSurface.labelRes(): Int = when (this) {
    GlassSurface.PANELS -> R.string.settings_glass_surface_panels
    GlassSurface.SIDEBAR -> R.string.settings_glass_surface_sidebar
    GlassSurface.PREVIEW -> R.string.settings_glass_surface_preview
    GlassSurface.DIALOGS -> R.string.settings_glass_surface_dialogs
    GlassSurface.TOPBAR -> R.string.settings_glass_surface_topbar
    GlassSurface.CARDS -> R.string.settings_glass_surface_cards
    GlassSurface.MINI_PLAYER -> R.string.settings_glass_surface_miniplayer
}

@Composable
private fun GlassPreset.labelRes(): Int = when (this) {
    GlassPreset.ULTRA_CLEAR -> R.string.settings_glass_preset_ultra_clear
    GlassPreset.CLEAR -> R.string.settings_glass_preset_clear
    GlassPreset.BALANCED -> R.string.settings_glass_preset_balanced
    GlassPreset.TINTED -> R.string.settings_glass_preset_tinted
    GlassPreset.OPAQUE -> R.string.settings_glass_preset_opaque
    GlassPreset.CUSTOM -> R.string.settings_glass_preset_custom
}

/** A card, a line of text and the accent — the three things every setting on this page changes. */
@Composable
private fun AppearancePreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
    ) {
        Column(Modifier.padding(MobileDimens.GapMedium)) {
            Text(
                text = stringResource(R.string.settings_panel_width_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_font_preview),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.padding(top = MobileDimens.GapSmall),
                horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                )
                Box(
                    Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp),
                        ),
                )
                Text(
                    text = stringResource(R.string.settings_focus_highlight_sample),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WeatherLocationField(vm: SettingsViewModel) {
    val stored = vm.settings.weatherLocation.pref("")
    var text by remember(stored) { mutableStateOf(stored) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            vm.edit { setWeatherLocation(it) }
        },
        singleLine = true,
        label = { Text(stringResource(R.string.settings_custom_location)) },
        placeholder = { Text(stringResource(R.string.settings_location_hint)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
    )
}

/** The ring's colour and thickness. Blank colour means "follow the accent", which is the default. */
@Composable
private fun HighlightSheet(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val isDark = vm.settings.themeMode.pref(ThemeMode.DARK) != ThemeMode.LIGHT
    val current = vm.settings.focusHighlight.pref("")
    val width = vm.settings.focusHighlightWidth.pref(2)
    MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_selection_highlight),
    ) {
        SettingsSlider(
            title = stringResource(R.string.settings_focus_thickness),
            value = width,
            range = 1..6,
            valueLabel = focusWidthLabel(width),
            onValueChange = { dp -> vm.edit { setFocusHighlightWidth(dp) } },
        )
        SettingRow(
            title = stringResource(R.string.settings_accent),
            checked = current.isBlank(),
            onCheckedChange = { vm.edit { setFocusHighlight("") } },
        )
        AccentColor.entries.forEach { color ->
            val hex = "#%06X".format(color.roles(isDark).primary and 0xFFFFFF)
            SettingRow(
                title = stringResource(color.labelRes),
                checked = current.equals(hex, ignoreCase = true),
                onCheckedChange = { vm.edit { setFocusHighlight(hex) } },
                modifier = Modifier,
            )
        }
    }
}

@Composable
private fun GlassSheet(vm: SettingsViewModel, glass: GlassConfig, onDismiss: () -> Unit) {
    MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_glass_effect),
    ) {
        SettingRow(
            title = stringResource(R.string.settings_glass_effect_title),
            subtitle = stringResource(R.string.settings_glass_master_description),
            checked = glass.enabled,
            // Off is an empty scope, so turning it back on has to put something in it: everything,
            // which is what the surface switches below then narrow down.
            onCheckedChange = { on ->
                val bits = if (on) GlassConfig(scope = GlassSurface.entries.toSet()).toBitmask() else 0
                vm.edit { setGlassScopeBitmask(bits) }
            },
        )
        if (glass.enabled) {
            GlassPreset.entries.filter { it != GlassPreset.CUSTOM }.forEach { preset ->
                SettingRow(
                    title = stringResource(preset.labelRes()),
                    checked = glass.preset == preset,
                    onCheckedChange = { vm.edit { setGlassPreset(preset) } },
                )
            }
            GlassSurface.entries.forEach { surface ->
                SettingRow(
                    title = stringResource(surface.labelRes()),
                    checked = surface in glass.scope,
                    onCheckedChange = { on ->
                        val scope =
                            if (on) glass.scope + surface else glass.scope - surface
                        vm.edit { setGlassScopeBitmask(GlassConfig(scope = scope).toBitmask()) }
                    },
                )
            }
        }
    }
}

@Composable
private fun FontsSheet(vm: SettingsViewModel, fonts: FontCustomization, onDismiss: () -> Unit) {
    MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_font_customization),
    ) {
        Text(
            text = stringResource(R.string.settings_font_preview),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = MobileDimens.ScreenPaddingH),
        )
        SettingsSlider(
            title = stringResource(R.string.settings_font_size),
            value = fonts.sizePercent,
            range = UiFontScale.MIN..UiFontScale.MAX,
            steps = stepsFor(UiFontScale.MIN, UiFontScale.MAX, UiFontScale.STEP),
            onValueChange = { pct ->
                vm.edit { setFontCustomization(fonts.copy(sizePercent = UiFontScale.clamp(pct))) }
            },
        )
        SettingsSlider(
            title = stringResource(R.string.settings_popup_font_size),
            subtitle = stringResource(R.string.settings_popup_font_size_description),
            value = fonts.popupFontSizePercent,
            range = PopupFontScale.MIN..PopupFontScale.MAX,
            steps = stepsFor(PopupFontScale.MIN, PopupFontScale.MAX, PopupFontScale.STEP),
            onValueChange = { pct ->
                vm.edit {
                    setFontCustomization(fonts.copy(popupFontSizePercent = PopupFontScale.clamp(pct)))
                }
            },
        )
        AppFontFamily.entries.forEach { family ->
            SettingRow(
                title = stringResource(family.labelRes()),
                checked = fonts.mainFamily == family,
                onCheckedChange = { vm.edit { setFontCustomization(fonts.copy(mainFamily = family)) } },
            )
        }
    }
}
