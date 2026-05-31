plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)

    id("tailwindcss")
}

dependencies {
    implementation(projects.discord.link)

    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.html)

    implementation(libs.cache)

    implementation(libs.logging)
}

tailwindcss {
    resourceTask = tasks.processResources
    resourcePath = "static"
}
