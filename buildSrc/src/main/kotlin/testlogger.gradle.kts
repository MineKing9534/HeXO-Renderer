import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.logger.SequentialTestLogger
import com.adarshr.gradle.testlogger.theme.Theme
import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    id("com.adarshr.test-logger")
}

val testTheme = ThemeType.MOCHA
fun TestLoggerExtension.configureTestLogging() {
    this.showFullStackTraces = true
    this.showExceptions = true
    this.showCauses = true
    this.showStackTraces = true
    this.showStandardStreams = false
    this.theme = testTheme
}

testlogger {
    configureTestLogging()
}

tasks.withType<AbstractTestTask>().configureEach {
    if (this is Test) {
        return@configureEach
    }

    testLogging.lifecycle.events = emptySet()

    val testLoggerExtension = TestLoggerExtension(project).apply {
        configureTestLogging()
    }

    val theme = testTheme.themeClass
        .getDeclaredConstructor(TestLoggerExtension::class.java)
        .apply { isAccessible = true }
        .newInstance(testLoggerExtension) as Theme

    val testLogger = SequentialTestLogger(logger, testLoggerExtension, theme)

    addTestListener(testLogger)
    addTestOutputListener(testLogger)
}
