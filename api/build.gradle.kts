plugins {
    id("kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.atomicfu)

    id("publish")
}

detekt {
    source = files("src/commonMain/kotlin", "src/jsMain/kotlin", "src/jvmMain/kotlin")
}

kotlin {
    withSourcesJar(publish = true)

    sourceSets.commonMain {
        dependencies {
            implementation(projects.core)

            implementation(libs.kotlin.serialization.json)
            implementation(libs.bundles.ktor.client)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(projects.board)

            implementation(libs.cache)
            implementation(libs.ktor.client.cio)

            implementation(libs.logging)

            implementation("io.socket:socket.io-client:2.1.1")
        }
    }

    sourceSets.jsMain {
        dependencies {
            implementation(libs.ktor.client.js)

            implementation(libs.logging)

            implementation(npm("socket.io-client", "4.8.3"))
        }
    }
}
