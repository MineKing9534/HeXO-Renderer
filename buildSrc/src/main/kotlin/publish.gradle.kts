import org.gradle.api.publish.maven.MavenPublication

plugins {
    `maven-publish`
}

val release = System.getenv("RELEASE") == "true"

publishing {
    repositories {
        maven {
            url = uri("https://maven.mineking.dev/" + (if (release) "releases" else "snapshots"))
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_SECRET")
            }
        }
    }

    publications {
        // only create for JVM modules that don't already have KMP publications
        if (findByName("kotlinMultiplatform") == null && findByName("jvm") == null) {
            create<MavenPublication>("mavenJvm") {
                from(components["kotlin"])
            }
        }
    }

    publications.withType<MavenPublication> {
        version = if (release) "${ project.version }" else System.getenv("BRANCH")
    }
}
