package com.dqc.egsengine.feature.scaffold.domain.model

/**
 * 页面模板数据模型
 */
data class PageTemplate(
    val pageName: String,
    val moduleName: String,
    val modulePackage: String,
    val useCases: List<UseCaseInfo>,
    val basePackage: String?,
    val baseClassPackages: BaseClassPackages,
)

/**
 * UseCase 参数信息
 */
data class UseCaseParam(
    val name: String,
    val type: String,
) {
    /** 生成占位符默认值 */
    val placeholderValue: String
        get() = when {
            type.endsWith("?") -> "null"
            type == "Long" -> "0L"
            type == "Int" -> "0"
            type == "String" -> "\"\""
            type == "Boolean" -> "false"
            type == "Double" -> "0.0"
            type == "Float" -> "0f"
            type.startsWith("List<") -> "emptyList()"
            else -> "null"
        }
}

/**
 * UseCase 信息
 */
data class UseCaseInfo(
    val name: String,
    val packageName: String,
    val path: String,
    val returnType: String? = null,
    val parameters: List<UseCaseParam> = emptyList(),
) {
    val camelName: String = name.replaceFirstChar { it.lowercase() }
        .replace("UseCase", "")
        .replaceFirstChar { it.lowercase() }
}

/**
 * 页面生成结果
 */
data class PageScaffoldResult(
    val pageName: String,
    val moduleName: String,
    val files: List<GeneratedFileInfo>,
    val dryRun: Boolean,
)

/**
 * 生成的文件信息
 */
data class GeneratedFileInfo(
    val path: String,
    val content: String,
)

/**
 * 页面配置（交互式或命令行输入）
 */
data class PageConfig(
    val moduleName: String,
    val pageName: String,
    val useCases: List<UseCaseInfo>,
)
