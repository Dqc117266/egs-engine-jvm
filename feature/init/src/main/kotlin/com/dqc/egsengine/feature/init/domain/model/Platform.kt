package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    ANDROID,
    KMP,
    KMP_ANDROID,
    SPRING_BOOT,
    VUE3,
    KOTLIN_JVM,
}
