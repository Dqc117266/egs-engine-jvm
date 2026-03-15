package com.dqc.egsengine.buildlogic

import com.dqc.egsengine.buildlogic.ext.implementation
import com.dqc.egsengine.buildlogic.ext.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class ApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("application")
                apply<KotlinConventionPlugin>()
                apply<SpotlessConventionPlugin>()
            }

            extensions.configure<JavaApplication> {
                mainClass.set("com.dqc.egsengine.AppKt")
            }

            dependencies {
                implementation(libs.kotlin.reflect)
                implementation(libs.coroutines.core)
                implementation(libs.bundles.koin)
                implementation(libs.bundles.logging)
                implementation(libs.bundles.retrofit)
                implementation(libs.clikt)
            }
        }
    }
}
