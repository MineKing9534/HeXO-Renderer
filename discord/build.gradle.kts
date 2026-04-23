import de.mineking.discord.localization.gradle.import

plugins {
    alias(libs.plugins.dtk.localization)
    alias(libs.plugins.shadow)

    id("application")
}

dependencies {
    implementation(projects.core)

    implementation(projects.parse)
    implementation(projects.render)

    implementation(projects.history)

    implementation(libs.kotlin.coroutines.core)

    implementation(libs.jda)
    implementation(libs.jda.emoji)
    implementation(libs.dtk)

    implementation(libs.logging)
    runtimeOnly(libs.logback)

    runtimeOnly(kotlin("reflect"))
}

discordLocalization {
    locales = listOf("en-US")
    defaultLocale = "en-US"

    localizationDirectory = "$projectDir/localization"
    locationFormat = "%locale%/%name%.yaml"

    botPackage = "de.mineking.hexo.discord"

    import("de.mineking.hexo.history.Match")
    import("de.mineking.hexo.history.TimeControl")
    import("de.mineking.hexo.history.GameFinishReason")
}

application {
    mainClass = "de.mineking.hexo.discord.MainKt"
}
