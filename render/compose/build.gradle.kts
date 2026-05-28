plugins {
    kotlin("multiplatform")
    id("kotlin-common")

    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.compiler)
}

repositories {
    google()
}

kotlin {
    js { browser() }


    sourceSets.jsMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.render)

            implementation(compose.html.core)
            implementation(compose.runtime)
        }
    }
}
