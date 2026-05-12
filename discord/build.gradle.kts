import de.mineking.discord.localization.gradle.import

plugins {
    id("kotlin-jvm")
    id("application")
    alias(libs.plugins.shadow)

    alias(libs.plugins.dtk.localization)
}

dependencies {
    implementation(projects.core)
    implementation(projects.board)

    implementation(projects.parse)
    implementation(projects.render)

    implementation(projects.api)

    implementation(libs.kotlin.coroutines.core)

    implementation(libs.cache)

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

    import("kotlin.math.roundToInt")

    import("de.mineking.hexo.api.utils.TimeControl")
    import("de.mineking.hexo.api.game.FinishedGame")
    import("de.mineking.hexo.api.game.GameFinishReason")
    import("de.mineking.hexo.api.profile.Profile")

    import("kotlin.time.toJavaInstant")
    import("net.dv8tion.jda.api.utils.TimeFormat")
}

application {
    mainClass = "de.mineking.hexo.discord.MainKt"
}
