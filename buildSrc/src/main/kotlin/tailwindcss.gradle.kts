import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("com.github.node-gradle.node")
}

node {
    download = true
    version = "22.11.0"
    npmVersion = "10.9.0"
}

abstract class TailwindExtension {
    abstract val resourceTask: Property<TaskProvider<out Copy>>
    abstract val resourcePath: Property<String>
}

val extension = extensions.create<TailwindExtension>("tailwindcss")

val generatedTailwindCss = layout.buildDirectory.file("generated/resources/tailwind/styles.css")
val tailwindInputCss = layout.projectDirectory.file("src/css/styles.css")

val tailwindTask = tasks.register<NpmTask>("generateTailwindCss") {
    group = "build"
    description = "Generates the tailwindcss file"

    inputs.files(
        fileTree("src") {
            include("**/*.css", "**/*.html", "**/*.kt")
        }
    )
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

afterEvaluate {
    extension.resourceTask.get().configure {
        dependsOn(tailwindTask)
        from(generatedTailwindCss) {
            into(extension.resourcePath)
        }
    }
}
