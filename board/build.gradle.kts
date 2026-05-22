plugins {
    id("kotlin-multiplatform")
    id("publish")
}

kotlin {
    sourceSets.commonMain {
        dependencies {
            implementation(projects.core)
        }
    }
}
