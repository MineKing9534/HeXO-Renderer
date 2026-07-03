import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    id("kotlin-common")
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        browser {
            testTask {
                failOnNoDiscoveredTests = false
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.named<KotlinJvmTest>("jvmTest") {
    useJUnitPlatform()
}

tasks.register("test") {
    description = "Runs all tests"
    dependsOn("jvmTest")
    dependsOn("jsTest")
}
