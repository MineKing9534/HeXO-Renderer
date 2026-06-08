import de.mineking.discord.localization.gradle.import

plugins {
    id("kotlin-jvm")

    alias(libs.plugins.dtk.localization)
}

dependencies {
    implementation(projects.board)

    implementation(projects.board.parse)
    implementation(projects.board.render)

    implementation(projects.api)
    implementation(projects.discord.link)
    runtimeOnly(libs.postgres)

    implementation(libs.kotlin.coroutines.core)

    implementation(libs.cache)

    implementation(libs.jda)
    implementation(libs.jda.emoji)
    implementation(libs.dtk)

    implementation(libs.logging)

    runtimeOnly(kotlin("reflect"))
}

discordLocalization {
    locales = listOf("en-US")
    defaultLocale = "en-US"

    localizationDirectory = "$projectDir/localization"
    locationFormat = "%locale%/%name%.yaml"

    botPackage = "de.mineking.hexo.bot"

    import("kotlin.math.roundToInt")

    import("de.mineking.hexo.api.utils.TimeControl")
    import("de.mineking.hexo.api.game.FinishedGame")
    import("de.mineking.hexo.api.game.GameFinishReason")
    import("de.mineking.hexo.api.profile.RichProfile")

    import("kotlin.time.toJavaInstant")
    import("net.dv8tion.jda.api.utils.TimeFormat")
}
