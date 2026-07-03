plugins {
    id("kotlin-common")
    kotlin("jvm")
}

tasks.test {
    useJUnitPlatform()
}
