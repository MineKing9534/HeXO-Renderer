plugins {
    id("dev.detekt")
}

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:${detekt.toolVersion.get()}")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt.yml")
}
