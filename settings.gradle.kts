pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://maven.mineking.dev/snapshots")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "HeXO"

include(":core")

include(":render")
include(":parse")

include(":history")

include(":discord")
