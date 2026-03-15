package com.dqc.egsengine.feature.analyzer.domain.model

data class ProjectInfo(
    val name: String,
    val rootPath: String,
    val gradleVersion: String = "unknown",
    val kotlinVersion: String? = null,
    val javaVersion: String? = null,
    val compileSdk: String? = null,
    val minSdk: String? = null,
    val targetSdk: String? = null,
    val modules: List<ModuleInfo> = emptyList(),
) {
    val isAndroidProject: Boolean
        get() = modules.any { it.type == ProjectType.ANDROID_APPLICATION || it.type == ProjectType.ANDROID_LIBRARY }

    val isKmpProject: Boolean
        get() = modules.any { it.type == ProjectType.KOTLIN_MULTIPLATFORM }

    val overallType: String
        get() = when {
            isKmpProject && isAndroidProject -> "Kotlin Multiplatform (with Android)"
            isKmpProject -> "Kotlin Multiplatform"
            isAndroidProject -> "Android"
            modules.any { it.type == ProjectType.KOTLIN_JVM } -> "Kotlin JVM"
            modules.any { it.type == ProjectType.JAVA } -> "Java"
            else -> "Unknown"
        }

    val moduleCountByType: Map<ProjectType, Int>
        get() = modules.groupBy { it.type }.mapValues { it.value.size }
}
