package com.dqc.egsengine.feature.init.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseClassInfo(
    // Android/KMP fields (authoritative for non-Godot projects).
    val name: String = "",
    val packageName: String = "",
    val module: String = "",
    val filePath: String = "",
    val kind: BaseClassKind = BaseClassKind.OPEN_CLASS,
    // Godot fields (authoritative for Platform.GODOT projects). Tolerated on Android/KMP via defaults.
    @SerialName("className") val godotClassName: String? = null,
    @SerialName("scriptPath") val godotScriptPath: String? = null,
    @SerialName("type") val godotNodeType: String? = null,
)

@Serializable
enum class BaseClassKind {
    ABSTRACT_CLASS,
    OPEN_CLASS,
}
