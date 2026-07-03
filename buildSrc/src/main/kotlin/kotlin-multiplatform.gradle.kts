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

    sourceSets
        .matching { "Test" in it.name && "jvm" !in it.name }
        .configureEach {
            dependencies {
                implementation(kotlin("test"))
            }
        }

    sourceSets.named("jvmTest") {
        dependencies {
            CommonConfig.JVM_TEST_DEPENDENCIES.forEach { implementation(it) }
            CommonConfig.JVM_TEST_RUNTIME_DEPENDENCIES.forEach { runtimeOnly(it) }
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
