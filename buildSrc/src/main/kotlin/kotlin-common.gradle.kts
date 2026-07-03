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

private val javaVersion = 21
private val commonKotlinCompilerArgs = listOf(
    "-Xexpect-actual-classes",
    "-Xreturn-value-checker=full",
)

private val junitVersion = "6.1.1"
private val jvmTestDependencies = listOf(
    "org.junit.jupiter:junit-jupiter-api:$junitVersion",
    "org.junit.jupiter:junit-jupiter-params:$junitVersion",
)
private val jvmTestRuntimeDependencies = listOf(
    "org.junit.jupiter:junit-jupiter-engine:$junitVersion",
)

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        jvmToolchain(javaVersion)
        compilerOptions.freeCompilerArgs.addAll(commonKotlinCompilerArgs)
    }

    dependencies {
        jvmTestDependencies.forEach { add("testImplementation", it) }
        jvmTestRuntimeDependencies.forEach { add("testRuntimeOnly", it) }
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension>("kotlin") {
        jvmToolchain(javaVersion)
        compilerOptions.freeCompilerArgs.addAll(commonKotlinCompilerArgs)

        jvm().compilations["test"].defaultSourceSet.dependencies {
            jvmTestDependencies.forEach { implementation(it) }
            jvmTestRuntimeDependencies.forEach { runtimeOnly(it) }
        }
    }
}
