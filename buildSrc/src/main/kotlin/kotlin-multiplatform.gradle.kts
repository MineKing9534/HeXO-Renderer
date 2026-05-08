plugins {
    id("kotlin-common")
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js { browser() }
}
