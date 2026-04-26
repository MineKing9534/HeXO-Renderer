dependencies {
    implementation(projects.core)

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.cache)

    testImplementation(projects.parse)
}
