import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.node)
}

dependencies {
    implementation(projects.discord.link)

    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.html)

    implementation(libs.cache)

    implementation(libs.logging)
}

val generatedTailwindCss = layout.buildDirectory.file("generated/resources/tailwind/styles.css")
val tailwindInputCss = layout.projectDirectory.file("src/main/resources/static/styles.css")

node {
    download = true
    version = "22.11.0"
    npmVersion = "10.9.0"
}

val tailwindTask = tasks.register<NpmTask>("generateTailwindCss") {
    group = "build"
    description = "Generates the tailwindcss file"

    inputs.files(
        fileTree("src/main/kotlin") {
            include("**/*.kt")
        }
    )

    inputs.file(tailwindInputCss)
    outputs.file(generatedTailwindCss)

    doFirst {
        generatedTailwindCss.get().asFile.parentFile.mkdirs()
    }

    args.set(listOf(
        "exec",
        "--yes",
        "--package",
        "@tailwindcss/cli@4.3.0",
        "--",
        "tailwindcss",
        "-i", tailwindInputCss.asFile.absolutePath,
        "-o", generatedTailwindCss.get().asFile.absolutePath,
        "--minify",
    ))
}

tasks.processResources {
    dependsOn(tailwindTask)

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(generatedTailwindCss) {
        into("static")
    }
}
