import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.dqc.egsengine.buildlogic"

/*
Configure the build-logic plugins to target JDK from version catalog
This matches the JDK used to build the project.
*/
val javaVersion =
    libs
        .versions
        .java
        .get()

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion)
    }

    jvmToolchain(javaVersion.toInt())
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.test.logger.gradlePlugin)

    /*
    Expose generated type-safe version catalogs accessors accessible from precompiled script plugins
    e.g. add("implementation", libs.koin)
    https://github.com/gradle/gradle/issues/15383
     */
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("applicationConvention") {
            id = "com.dqc.egsengine.convention.application"
            implementationClass = "com.dqc.egsengine.buildlogic.ApplicationConventionPlugin"
        }

        register("featureConvention") {
            id = "com.dqc.egsengine.convention.feature"
            implementationClass = "com.dqc.egsengine.buildlogic.FeatureConventionPlugin"
        }

        register("libraryConvention") {
            id = "com.dqc.egsengine.convention.library"
            implementationClass = "com.dqc.egsengine.buildlogic.LibraryConventionPlugin"
        }

        register("kotlinConvention") {
            id = "com.dqc.egsengine.convention.kotlin"
            implementationClass = "com.dqc.egsengine.buildlogic.KotlinConventionPlugin"
        }

        register("testConvention") {
            id = "com.dqc.egsengine.convention.test"
            implementationClass = "com.dqc.egsengine.buildlogic.TestConventionPlugin"
        }

        register("spotlessConvention") {
            id = "com.dqc.egsengine.convention.spotless"
            implementationClass = "com.dqc.egsengine.buildlogic.SpotlessConventionPlugin"
        }

        register("detektConvention") {
            id = "com.dqc.egsengine.convention.detekt"
            implementationClass = "com.dqc.egsengine.buildlogic.DetektConventionPlugin"
        }
    }
}
