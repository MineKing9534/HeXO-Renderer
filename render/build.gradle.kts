plugins {
    id("kotlin-jvm-toolchain")
}

dependencies {
    implementation(projects.core)
    implementation(projects.board)

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.cache)

    testImplementation(projects.parse)
}
