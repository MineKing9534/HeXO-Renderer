import org.gradle.internal.execution.caching.CachingState.enabled

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
    js {
        browser {
            binaries.executable()
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }


    sourceSets.jsMain {
        dependencies {
            implementation(projects.board)
            implementation(projects.render)
            implementation(projects.render.compose)

            implementation(projects.parse)

            implementation(compose.html.core)
            implementation(compose.runtime)

            implementation(npm("tailwindcss", "^4.3.0"))
            implementation(npm("@tailwindcss/postcss", "^4.3.0"))
            implementation(npm("postcss", "8.5.15"))
            implementation(npm("postcss-loader", "8.2.1"))
        }
    }
}
