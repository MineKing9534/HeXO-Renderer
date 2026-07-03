object CommonConfig {
    const val JAVA_VERSION = 21

    val COMMON_COMPILER_ARGS = listOf(
        "-Xexpect-actual-classes",
        "-Xreturn-value-checker=full",
    )

    private const val JUNIT_VERSION = "6.1.1"
    val JVM_TEST_DEPENDENCIES = listOf(
        "org.junit.jupiter:junit-jupiter-api:$JUNIT_VERSION",
        "org.junit.jupiter:junit-jupiter-params:$JUNIT_VERSION",
    )
    val JVM_TEST_RUNTIME_DEPENDENCIES = listOf(
        "org.junit.jupiter:junit-jupiter-engine:$JUNIT_VERSION",
    )
}
