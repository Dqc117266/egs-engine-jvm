package com.dqc.egsengine.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("java-library")
                apply<KotlinConventionPlugin>()
                apply<TestConventionPlugin>()
            }
        }
    }
}
