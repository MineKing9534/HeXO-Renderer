import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("kotlin-multiplatform")

    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.compiler)

    id("tailwindcss")
}

repositories {
    google()
}

val webBasePath = providers.gradleProperty("web.basePath")
    .orElse("/")
    .map { path ->
        val prefixed = if (path.startsWith("/")) path else "/$path"
        prefixed.removeSuffix("/")
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
            implementation(projects.api)

            implementation(compose.html.core)
            implementation(compose.html.svg)
            implementation(compose.runtime)
        }
    }
}

tailwindcss {
    resourceTask = tasks.jsProcessResources
    resourcePath = "/"
}

tasks.named<Copy>("jsProcessResources") {
    inputs.property("webBasePath", webBasePath)

    filesMatching("index.html") {
        filter<ReplaceTokens>(
            "tokens" to mapOf("WEB_BASE_PATH" to webBasePath.get()),
        )
    }
}
