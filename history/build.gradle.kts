plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.core)

            implementation(libs.kotlin.serialization.json)
            implementation(libs.bundles.ktor.client)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(projects.board)
            implementation(libs.cache)
        }
    }
}
