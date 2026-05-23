plugins {
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
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
