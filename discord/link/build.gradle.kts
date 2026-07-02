plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.hds)

    implementation(libs.jda)
    implementation(libs.dtk)

    implementation(libs.bundles.exposed)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)
}
