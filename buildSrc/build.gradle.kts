plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.build.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.testlogger.gradle.plugin)
    implementation(libs.node.gradle.plugin)
}
