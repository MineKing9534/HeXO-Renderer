plugins {
    id("kotlin-multiplatform")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.board)
            compileOnly(projects.hds)
        }
    }

    sourceSets.jvmMain {
        dependencies {
            implementation(libs.cache)
        }
    }
}
