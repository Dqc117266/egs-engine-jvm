rootProject.name = "egs-engine"

include(
    ":app",
    ":feature:base",
    ":feature:common",
    ":feature:command",
    ":feature:task",
    ":feature:script",
    ":feature:analyzer",
    ":feature:init",
    ":feature:scaffold",
    ":konsist-test",
)

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://jitpack.io")
        maven("https://repo.gradle.org/gradle/libs-releases/")
    }
}

// Generate type safe accessors when referring to other projects eg.
// Before: implementation(project(":feature:command"))
// After: implementation(projects.feature.command)
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
