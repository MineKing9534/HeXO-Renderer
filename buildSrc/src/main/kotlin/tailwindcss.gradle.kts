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

val tailwindVersion = "4.3.0"
val generatedTailwindCss = layout.buildDirectory.file("generated/resources/tailwind/styles.css")
val tailwindInputCss = layout.projectDirectory.file("src/css/styles.css")

val installTailwindCss = tasks.register<NpmTask>("installTailwindCss") {
    group = "build"
    description = "Installs tailwindcss packages used by the CSS generation task"

    inputs.property("tailwindVersion", tailwindVersion)
    outputs.dir(layout.projectDirectory.dir("node_modules/@tailwindcss/cli"))
    outputs.dir(layout.projectDirectory.dir("node_modules/tailwindcss"))

    args.set(listOf(
        "install",
        "--no-save",
        "--package-lock=false",
        "@tailwindcss/cli@$tailwindVersion",
        "tailwindcss@$tailwindVersion",
    ))
}

val tailwindTask = tasks.register<NpmTask>("generateTailwindCss") {
    group = "build"
    description = "Generates the tailwindcss file"

    dependsOn(installTailwindCss)

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
