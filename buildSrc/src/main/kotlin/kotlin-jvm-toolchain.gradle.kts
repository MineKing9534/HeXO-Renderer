plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.mineking.dev/releases")
    maven("https://maven.mineking.dev/snapshots")
}

kotlin {
    jvmToolchain(21)
    compilerOptions.freeCompilerArgs.addAll(
        "-opt-in=kotlin.uuid.ExperimentalUuidApi",
        "-Xexplicit-backing-fields",
        "-Xcontext-parameters",
    )
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
