import versioning.BuildConfig

plugins {
    `java-library`
    id("io.freefair.lombok")
    id("com.diffplug.spotless")
}

group = rootProject.group
version = rootProject.version
description = rootProject.description

// Java compilation settings
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
    withSourcesJar()
    withJavadocJar()
}

// Lombok configuration
//lombok {
//    version.set("1.18.30") // Use the version from your version catalog if available
//}

// Spotless configuration
spotless {
    java {
        endWithNewline()
        indentWithSpaces(4)
        removeUnusedImports()
        trimTrailingWhitespace()
        targetExclude("build/generated/**/*")
    }

    kotlinGradle {
        endWithNewline()
        indentWithSpaces(4)
        trimTrailingWhitespace()
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    build {
        // Ensure spotlessApply runs before build
        dependsOn(tasks.named("spotlessApply"))
    }

    // Process resources (e.g., for plugin metadata files)
    processResources {
        val properties = mapOf(
            "timestamp" to rootProject.ext["timestamp"],
            "version" to project.version.toString(),
            "build_shade_pe" to BuildConfig.shadePE,
            "build_relocate" to BuildConfig.relocate,
            "build_release" to BuildConfig.release,
        )

        properties.forEach { (key, value) -> inputs.property(key, value) }

        filesMatching(
            listOf(
                "bungee.yml",
                "velocity-plugin.json",
                "watchneko.properties"
            )
        ) {
            expand(properties)
        }
    }

    javadoc {
        title = "${rootProject.name}-${project.name} v${rootProject.version}"
        options.encoding = "UTF-8"
        options.overview =
            rootProject.file("buildSrc/src/main/resources/javadoc-overview.html").toString()
        setDestinationDir(file("${project.layout.buildDirectory.asFile.get()}/docs/javadoc"))
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }
}

// Default tasks
defaultTasks("build")
