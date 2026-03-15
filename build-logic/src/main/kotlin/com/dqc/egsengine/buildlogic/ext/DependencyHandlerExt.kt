package com.dqc.egsengine.buildlogic.ext

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

private const val IMPLEMENTATION = "implementation"
private const val TEST_IMPLEMENTATION = "testImplementation"
private const val TEST_RUNTIME_ONLY = "testRuntimeOnly"

fun DependencyHandlerScope.implementation(provider: Provider<out Any>) {
    add(IMPLEMENTATION, provider)
}

fun DependencyHandlerScope.implementation(project: Project) {
    add(IMPLEMENTATION, project)
}

fun DependencyHandlerScope.testImplementation(project: Project) {
    add(TEST_IMPLEMENTATION, project)
}

fun DependencyHandlerScope.testImplementation(provider: Provider<out Any>) {
    add(TEST_IMPLEMENTATION, provider)
}

fun DependencyHandlerScope.testRuntimeOnly(provider: Provider<out Any>) {
    add(TEST_RUNTIME_ONLY, provider)
}
