import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // Kotlin is provided by AGP 9's built-in Kotlin support; this plugin pins the Compose compiler
    // to the same version the TV app and core use.
    alias(libs.plugins.compose.compiler)
}

android {
    // Distinct from the TV app's tv.own.owntv, so both install side by side on one device and each
    // keeps its own data. This value can never change after the first release — the launcher label
    // and icon can.
    namespace = "tv.own.owntv.mobile"
    compileSdk {
        version = release(37)
    }

    // Signing credentials AND local-only build switches, kept in a standalone properties file OUTSIDE
    // the repo. Gradle only reads gradle.properties from GRADLE_USER_HOME or the project dir, so this
    // one is loaded by hand. Declared here because defaultConfig below already needs it.
    val localSigningProps = Properties().apply {
        val f = File("E:/MEGA/CODE/OwnTV_Gradle/owntv-signing.properties")
        if (f.isFile) f.inputStream().use { load(it) }
    }

    defaultConfig {
        applicationId = "tv.own.owntv.mobile"
        minSdk = 26
        targetSdk = 36
        // CI injects these from the git tag, exactly as in the TV app. The fallbacks are only used
        // by local/debug builds and are pinned HIGH so a dev APK is always "newer" than a published
        // release and installs straight over it.
        versionCode = (System.getenv("VERSION_CODE") ?: "99999").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "99.99.99"

        // The three switches core reads through CoreBuildInfo. Same resolution order as the TV app:
        // env var (CI) > Gradle property > the out-of-repo properties file.
        buildConfigField(
            "boolean",
            "DIAGNOSTIC_BUILD",
            (providers.gradleProperty("diagnosticBuild").orNull == "true").toString(),
        )
        buildConfigField(
            "boolean",
            "DEV_TOOLS",
            (
                (
                    providers.gradleProperty("owntv.devTools").orNull
                        ?: localSigningProps.getProperty("owntv.devTools")
                    ) == "true"
                ).toString(),
        )
        // Shared secret the metadata Worker's edge rule requires (`x-owntv-key`). NEVER in the repo.
        // A blank key is a working configuration: core falls back to the unprotected base URL.
        val edgeKey = System.getenv("OWNTV_EDGE_KEY")
            ?: providers.gradleProperty("owntv.edgeKey").orNull
            ?: localSigningProps.getProperty("owntv.edgeKey")
            ?: ""
        buildConfigField("String", "TMDB_EDGE_KEY", "\"${edgeKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ABI split via product flavors, mirroring the TV app: `standard` is what real phones and tablets
    // run, `x86_64` exists for the emulator. The player engine ships large prebuilt .so files, so a
    // universal APK would be roughly double the size for no one's benefit.
    flavorDimensions += "abi"
    productFlavors {
        create("standard") {
            dimension = "abi"
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        }
        create("x86_64") {
            dimension = "abi"
            ndk { abiFilters += listOf("x86_64") }
        }
    }

    // The SAME keystore and the same property names as the TV app. A signing certificate must be
    // stable per applicationId, not unique per app, and these two apps are maintained by one person
    // — a second keystore would only add a second thing to lose. Env vars first (that is how CI
    // injects the GitHub secrets), then user-wide Gradle properties, then the out-of-repo file.
    // Nothing configured (a fork, a fresh clone) still builds; the APK is just unsigned.
    fun signingValue(env: String, property: String): String? =
        System.getenv(env)
            ?: providers.gradleProperty(property).orNull
            ?: localSigningProps.getProperty(property)

    val releaseKeystore = signingValue("KEYSTORE_FILE", "owntv.keystoreFile")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = signingValue("KEYSTORE_PASSWORD", "owntv.keystorePassword")
                keyAlias = signingValue("KEY_ALIAS", "owntv.keyAlias")
                keyPassword = signingValue("KEY_PASSWORD", "owntv.keyPassword")
            }
        }
    }

    testOptions {
        // JVM unit tests reach android.util.Log / SystemClock through core; return defaults instead
        // of "not mocked" crashes.
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // Every .so here is an already-stripped prebuilt from a dependency, so AGP's strip step
            // has nothing to remove and merely fails loudly on a machine with no NDK.
            keepDebugSymbols += "**/*.so"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // The shared engine, from its own repository — https://github.com/ahXN00/OwnTV_Core. Set
    // owntv.corePath in ~/.gradle/gradle.properties to build against its source instead of the pin.
    implementation(libs.owntv.core)
    // The shared playback engine. It renders nothing itself, which is exactly why the same engine
    // serves the TV HUD and this app's touch controls. Always on core's version.
    implementation(libs.owntv.player.core)
    // ...and libmpv itself, because OwnTVPlayer's supertype is MPVLib.EventObserver. Core now
    // exposes it as `api`, so a local build against core's source no longer needs this line — but
    // the PINNED artifact below 1.0.6 still hides it behind `implementation`, and CI builds from the
    // pin. DELETE this and the catalog entry once owntvCore is 1.0.6 or newer.
    implementation(libs.libmpv)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose (BOM-managed) — Material 3 for touch. Never androidx.tv.*; see libs.versions.toml.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    // Window size classes — this app has to lay out for a phone and a tablet from one build.
    implementation(libs.androidx.compose.adaptive)

    // Lifecycle / Navigation
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // WorkManager — core's sync/EPG workers, whose auto-initializer core's manifest removes.
    implementation(libs.androidx.work.runtime)

    // Paging — core's catalog DAOs return PagingSource.
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Dependency injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
