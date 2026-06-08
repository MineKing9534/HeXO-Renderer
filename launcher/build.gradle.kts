plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)

    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(projects.api)
    implementation(projects.discord.bot)
    implementation(projects.discord.link)
    implementation(projects.server)

    implementation(projects.board.parse)
    implementation(projects.board.render)

    implementation(libs.kotlin.serialization.properties)

    implementation(libs.logging)
    runtimeOnly(libs.logback)
}

application {
    mainClass = "de.mineking.hexo.launcher.MainKt"
}
