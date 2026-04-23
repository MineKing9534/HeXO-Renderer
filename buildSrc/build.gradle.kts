plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.build.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
}
