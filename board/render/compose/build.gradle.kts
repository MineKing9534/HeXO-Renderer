plugins {
    kotlin("multiplatform")
    id("kotlin-common")

    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.compiler)

    id("publish")
}

repositories {
    google()
}

kotlin {
    js { browser() }

    sourceSets.jsMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.board.render)

            implementation(compose.html.core)
            implementation(compose.runtime)
        }
    }
}
