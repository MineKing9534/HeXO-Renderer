plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.api)

    implementation(libs.bundles.exposed)
}
