package com.dqc.egsengine.buildlogic

import com.dqc.egsengine.buildlogic.config.JavaBuildConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

class KotlinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            kotlinExtension.jvmToolchain(JavaBuildConfig.JVM_TOOLCHAIN_VERSION)
        }
    }
}
