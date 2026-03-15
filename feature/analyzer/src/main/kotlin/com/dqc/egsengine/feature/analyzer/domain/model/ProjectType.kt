package com.dqc.egsengine.feature.analyzer.domain.model

enum class ProjectType(val displayName: String) {
    ANDROID_APPLICATION("Android Application"),
    ANDROID_LIBRARY("Android Library"),
    KOTLIN_MULTIPLATFORM("Kotlin Multiplatform"),
    KOTLIN_JVM("Kotlin JVM"),
    JAVA("Java"),
    UNKNOWN("Unknown"),
}
