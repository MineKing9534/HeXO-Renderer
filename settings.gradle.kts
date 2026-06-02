pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.mineking.dev/snapshots")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "HeXO"

include(":core")
include(":board")

include(":parse")
include(":render")

include(":render:compose")
include(":web")

include(":api")
include(":api:processor")

include(":discord:link")
include(":discord:bot")

include(":server")
include(":launcher")
