package com.dqc.egsengine.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import com.dqc.egsengine.buildlogic.ext.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("**/*.kt", "**/*.kts")

                    val customRuleSets =
                        listOf(
                            libs.ktlint.ruleset.standard,
                        ).map {
                            it.get().toString()
                        }

                    ktlint()
                        .customRuleSets(customRuleSets)

                    endWithNewline()
                }

                // Don't add spotless as dependency for the Gradle's check task
                isEnforceCheck = false
            }
        }
    }
}
