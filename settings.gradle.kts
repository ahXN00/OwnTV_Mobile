pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // tv.own.owntv:core and :player-core, built from https://github.com/ahXN00/OwnTV_Core.
        // GitHub's Maven registry demands credentials even for a public package, so resolution needs
        // a token with read:packages — put it in ~/.gradle/gradle.properties as gpr.user / gpr.token,
        // NEVER in this repo. CI passes the same values through GITHUB_ACTOR / GPR_TOKEN.
        maven {
            name = "OwnTVCore"
            url = uri("https://maven.pkg.github.com/ahXN00/OwnTV_Core")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.token")
                    .orElse(providers.environmentVariable("GPR_TOKEN")).orNull
            }
            content { includeGroup("tv.own.owntv") }
        }
    }
}

// Local development: build against core's own source instead of the published artifact, so a core
// edit reaches this app with no publish step. Gradle substitutes the dependency automatically
// because OwnTV_Core publishes under the same group and artifact ids this app asks for. CI leaves
// owntv.corePath unset and resolves the pinned version instead. The SAME property serves the TV app
// and this one — set it once in ~/.gradle/gradle.properties, never here:
//   owntv.corePath=E:/MEGA/CODE/AI/OwnTV_Suite/OwnTV_Core
providers.gradleProperty("owntv.corePath").orNull?.takeIf { it.isNotBlank() }?.let { includeBuild(it) }

rootProject.name = "OwnTVMobile"
include(":app")
