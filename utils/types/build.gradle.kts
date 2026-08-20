plugins {
    id("kotlin-multiplatform")
    id("kotlin-latex")
    id("publish")

    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(libs.kotlin.serialization.core)
        }
    }
}
