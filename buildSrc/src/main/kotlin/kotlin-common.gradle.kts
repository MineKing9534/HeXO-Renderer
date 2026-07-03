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


pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        jvmToolchain(CommonConfig.JAVA_VERSION)
        compilerOptions.freeCompilerArgs.addAll(CommonConfig.COMMON_COMPILER_ARGS)
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        jvmToolchain(CommonConfig.JAVA_VERSION)
        compilerOptions.freeCompilerArgs.addAll(CommonConfig.COMMON_COMPILER_ARGS)
    }
}
