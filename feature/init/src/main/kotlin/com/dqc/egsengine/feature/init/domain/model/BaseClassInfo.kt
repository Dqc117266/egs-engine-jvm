package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BaseClassInfo(
    val name: String,
    val packageName: String,
    val module: String,
    val filePath: String,
    val kind: BaseClassKind,
)

@Serializable
enum class BaseClassKind {
    ABSTRACT_CLASS,
    OPEN_CLASS,
}
