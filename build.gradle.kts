// Root build file for the OwnTV mobile app. Everything the app is built on — the database, sync,
// EPG, backup, settings storage, the playback engine and every user-visible string — comes from the
// core library repository (ahXN00/OwnTV_Core); this repo holds the phone/tablet shell only.
plugins {
    alias(libs.plugins.android.application) apply false
    // Kotlin comes from AGP 9's built-in Kotlin support. The Compose compiler plugin is pinned to
    // that Kotlin version, exactly as in the TV app and in core.
    alias(libs.plugins.compose.compiler) apply false
}
