package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam
import org.slf4j.LoggerFactory
import java.io.File

/**
 * UseCase 扫描器 - 扫描指定模块中的 UseCase 文件
 */
class UseCaseScanner {
    private val logger = LoggerFactory.getLogger(UseCaseScanner::class.java)

    /**
     * 扫描指定模块中的所有 UseCase
     */
    fun scanByModule(projectRoot: File, moduleName: String): List<UseCaseInfo> {
        val moduleDir = projectRoot.resolve("feature/$moduleName")
        if (!moduleDir.exists()) {
            logger.warn("Module directory not found: feature/$moduleName")
            return emptyList()
        }

        val useCaseDir = moduleDir.resolve("src/main/kotlin")
        if (!useCaseDir.exists()) {
            logger.warn("Kotlin source directory not found in module: $moduleName")
            return emptyList()
        }

        return scanDirectory(useCaseDir, projectRoot)
    }

    /**
     * 扫描整个项目中的所有 UseCase
     */
    fun scanAll(projectRoot: File): Map<String, List<UseCaseInfo>> {
        val featureDir = projectRoot.resolve("feature")
        if (!featureDir.exists()) {
            logger.warn("Feature directory not found")
            return emptyMap()
        }

        val result = mutableMapOf<String, List<UseCaseInfo>>()
        featureDir.listFiles()?.filter { it.isDirectory }?.forEach { moduleDir ->
            val useCases = scanByModule(projectRoot, moduleDir.name)
            if (useCases.isNotEmpty()) {
                result[moduleDir.name] = useCases
            }
        }

        return result
    }

    /**
     * 列出所有可用的模块
     */
    fun listModules(projectRoot: File): List<String> {
        val featureDir = projectRoot.resolve("feature")
        if (!featureDir.exists()) return emptyList()

        return featureDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    private fun scanDirectory(dir: File, projectRoot: File): List<UseCaseInfo> {
        val useCases = mutableListOf<UseCaseInfo>()

        dir.walkTopDown()
            .filter { it.isFile && it.name.endsWith("UseCase.kt") }
            .forEach { file ->
                try {
                    val info = extractUseCaseInfo(file, projectRoot)
                    useCases.add(info)
                } catch (e: Exception) {
                    logger.warn("Failed to parse UseCase file: ${file.path}", e)
                }
            }

        return useCases.sortedBy { it.name }
    }

    private fun extractUseCaseInfo(file: File, projectRoot: File): UseCaseInfo {
        val packageName = extractPackageName(file)
        val relativePath = file.relativeTo(projectRoot).path
        val returnType = extractReturnType(file)
        val parameters = extractInvokeParameters(file)

        return UseCaseInfo(
            name = file.nameWithoutExtension,
            packageName = packageName,
            path = relativePath,
            returnType = returnType,
            parameters = parameters,
        )
    }

    private fun extractInvokeParameters(file: File): List<UseCaseParam> {
        val content = file.readText()
        val invokeMatch = Regex("""(?:suspend\s+)?(?:operator\s+)?fun\s+invoke\s*\(([\s\S]*?)\)\s*:""").find(content) ?: return emptyList()
        val paramsBlock = invokeMatch.groupValues.getOrNull(1)?.trim() ?: return emptyList()
        if (paramsBlock.isEmpty()) return emptyList()

        return paramsBlock.split(",").mapNotNull { part ->
            val paramMatch = Regex("""(\w+)\s*:\s*([^,\n=]+)""").find(part.trim())
            paramMatch?.let {
                UseCaseParam(
                    name = it.groupValues[1].trim(),
                    type = it.groupValues[2].trim(),
                )
            }
        }
    }

    private fun extractReturnType(file: File): String? {
        val content = file.readText()
        // 匹配 invoke 函数返回类型: ): Result<SseEmitter> = 或 ): SseEmitter =
        val regex = Regex("""(?:suspend\s+)?(?:operator\s+)?fun\s+invoke\s*\([^)]*\)\s*:\s*([^\s=]+)\s*=""")
        return regex.find(content)?.groupValues?.get(1)?.trim()
    }

    private fun extractPackageName(file: File): String {
        val content = file.readText()

        // 从 package 声明中提取包名
        val packageRegex = Regex("""package\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\.[a-zA-Z_][a-zA-Z0-9_]*)*)""")
        val match = packageRegex.find(content)

        return match?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Cannot extract package name from ${file.path}")
    }
}
