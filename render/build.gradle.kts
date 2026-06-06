plugins {
    id("kotlin-multiplatform")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)

            implementation(libs.svg)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
        }
    }

    sourceSets.commonTest {
        dependencies {
            implementation(projects.parse)
        }
    }
}
