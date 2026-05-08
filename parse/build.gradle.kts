plugins {
    id("kotlin-jvm")
}

dependencies {
    implementation(projects.core)
    implementation(projects.board)

    implementation(libs.cache)
}
