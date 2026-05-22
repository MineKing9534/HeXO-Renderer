plugins {
    id("kotlin-multiplatform")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.core)
            implementation(projects.board)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
        }
    }
}
