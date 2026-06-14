package com.dqc.egsengine.feature.analyzer.presentation

import com.dqc.egsengine.feature.analyzer.domain.model.ModuleInfo
import com.dqc.egsengine.feature.analyzer.domain.model.ProjectInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * analyze --json 的输出 DTO。从 [ProjectInfo] 映射，避免在 domain model 上加 @Serializable
 * （model 含计算属性，直接序列化会报构造参数缺失）。
 */
@Serializable
data class AnalyzeJsonDto(
    val name: String,
    val rootPath: String,
    val gradleVersion: String,
    val kotlinVersion: String?,
    val javaVersion: String?,
    val overallType: String,
    val isAndroidProject: Boolean,
    val isKmpProject: Boolean,
    val compileSdk: String? = null,
    val minSdk: String? = null,
    val targetSdk: String? = null,
    val moduleCount: Int,
    val modules: List<ModuleDto>,
)

@Serializable
data class ModuleDto(
    val name: String,
    val type: String,
    val hasAndroidManifest: Boolean,
    val plugins: List<String>,
    val dependencies: List<String>,
    val sourceSets: List<String>,
)

private fun ModuleInfo.toDto() = ModuleDto(
    name = name,
    type = type.displayName,
    hasAndroidManifest = hasAndroidManifest,
    plugins = plugins,
    dependencies = dependencies,
    sourceSets = sourceSetDirs,
)

private fun ProjectInfo.toDto() = AnalyzeJsonDto(
    name = name,
    rootPath = rootPath,
    gradleVersion = gradleVersion,
    kotlinVersion = kotlinVersion,
    javaVersion = javaVersion,
    overallType = overallType,
    isAndroidProject = isAndroidProject,
    isKmpProject = isKmpProject,
    compileSdk = compileSdk,
    minSdk = minSdk,
    targetSdk = targetSdk,
    moduleCount = modules.size,
    modules = modules.map { it.toDto() },
)

private val analyzeJson = Json { prettyPrint = true }

/** 将 [ProjectInfo] 序列化为格式化 JSON（取代手拼，避免字符串转义错误）。 */
internal fun encodeAnalyzeJson(info: ProjectInfo): String =
    analyzeJson.encodeToString(AnalyzeJsonDto.serializer(), info.toDto())
