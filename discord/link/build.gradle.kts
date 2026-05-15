plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.api)

    implementation(libs.bundles.exposed)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)
}
