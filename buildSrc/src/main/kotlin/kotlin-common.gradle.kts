import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://maven.mineking.dev/releases")
    maven("https://maven.mineking.dev/snapshots")
}

private val commonKotlinCompilerArgs = listOf(
    "-opt-in=kotlin.uuid.ExperimentalUuidApi",
    "-Xexplicit-backing-fields",
    "-Xcontext-parameters",
)

private val javaVersion = 21

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        jvmToolchain(javaVersion)
        compilerOptions.freeCompilerArgs.addAll(commonKotlinCompilerArgs)
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        jvmToolchain(javaVersion)
        compilerOptions.freeCompilerArgs.addAll(commonKotlinCompilerArgs)
    }
}
