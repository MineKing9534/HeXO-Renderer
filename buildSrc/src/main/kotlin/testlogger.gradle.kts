import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    id("com.adarshr.test-logger")
}

testlogger {
    showFullStackTraces = true
    theme = ThemeType.MOCHA
}
