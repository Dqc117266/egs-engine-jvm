package com.ajd.egsengine.buildlogic

import com.ajd.egsengine.buildlogic.ext.implementation
import com.ajd.egsengine.buildlogic.ext.libs
import com.ajd.egsengine.buildlogic.ext.testImplementation
import com.ajd.egsengine.buildlogic.ext.testRuntimeOnly
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("java-library")
                apply<KotlinConventionPlugin>()
                apply<TestConventionPlugin>()
            }

            dependencies {
                when (project.path) {
                    ":feature:common" -> {
                        // common is the bottom layer, no auto dependencies
                    }

                    ":feature:base" -> {
                        implementation(project(":feature:common"))
                    }

                    else -> {
                        implementation(project(":feature:base"))
                        implementation(project(":feature:common"))
                    }
                }

                implementation(libs.kotlin.reflect)
                implementation(libs.coroutines.core)
                implementation(libs.bundles.koin)
                implementation(libs.bundles.logging)
                implementation(libs.bundles.retrofit)
                implementation(libs.gson)
                implementation(libs.clikt)

                testImplementation(libs.bundles.test)
                testRuntimeOnly(libs.junit.jupiter.engine)
            }
        }
    }
}
