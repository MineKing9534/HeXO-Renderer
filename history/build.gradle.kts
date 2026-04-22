plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core)

    implementation(libs.kotlin.serialization.json)

    implementation(libs.bundles.ktor.client)
    implementation(libs.kache)
}
