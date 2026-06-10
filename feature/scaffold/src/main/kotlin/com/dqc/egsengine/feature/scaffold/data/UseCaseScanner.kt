package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.init.data.GradleSourceRoots
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
    fun scanByModule(
        projectRoot: File,
        moduleName: String,
    ): List<UseCaseInfo> {
        val moduleDir = projectRoot.resolve("feature/$moduleName")
        if (!moduleDir.exists()) {
            logger.warn("Module directory not found: feature/$moduleName")
            return emptyList()
        }

        val kotlinRoots = GradleSourceRoots.orderedKotlinRoots(moduleDir)
        if (kotlinRoots.isEmpty()) {
            logger.warn("No Kotlin source roots in module: $moduleName")
            return emptyList()
        }

        return kotlinRoots
            .flatMap { scanDirectory(it, projectRoot) }
            .distinctBy { it.path }
            .sortedBy { it.name }
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
     * Fills [UseCaseInfo.returnType] by re-parsing the use case source file when it was missing
     * (e.g. older scans or edge-case parse failures). Idempotent when [returnType] is already set.
     */
    fun enrichReturnTypesIfMissing(
        projectRoot: File,
        useCases: List<UseCaseInfo>,
    ): List<UseCaseInfo> = useCases.map { uc ->
        if (!uc.returnType.isNullOrBlank()) return@map uc
        val f = runCatching { projectRoot.resolve(uc.path) }.getOrNull() ?: return@map uc
        if (!f.isFile) return@map uc
        val rt = extractReturnType(f) ?: return@map uc
        uc.copy(returnType = rt)
    }

    /**
     * 列出所有可用的模块
     */
    fun listModules(projectRoot: File): List<String> {
        val featureDir = projectRoot.resolve("feature")
        if (!featureDir.exists()) return emptyList()

        return featureDir
            .listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    private fun scanDirectory(
        dir: File,
        projectRoot: File,
    ): List<UseCaseInfo> {
        val useCases = mutableListOf<UseCaseInfo>()

        dir
            .walkTopDown()
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

    private fun extractUseCaseInfo(
        file: File,
        projectRoot: File,
    ): UseCaseInfo {
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
        val openParen = findInvokeOpenParen(content) ?: return emptyList()
        val paramsBlock = extractBalancedParenContent(content, openParen)?.trim() ?: return emptyList()
        if (paramsBlock.isEmpty()) return emptyList()

        return splitTopLevelCommaParams(paramsBlock).mapNotNull { part ->
            val paramMatch = Regex("""(\w+)\s*:\s*(.+)""").find(part.trim())
            paramMatch?.let {
                UseCaseParam(
                    name = it.groupValues[1].trim(),
                    type =
                    it.groupValues[2]
                        .trim()
                        .removeSuffix(",")
                        .trim(),
                )
            }
        }
    }

    private fun extractReturnType(file: File): String? {
        val content = file.readText()
        val openParen = findInvokeOpenParen(content) ?: return null
        val closeParen = findMatchingCloseParen(content, openParen) ?: return null
        var i = closeParen + 1
        while (i < content.length && content[i].isWhitespace()) i++
        if (i >= content.length || content[i] != ':') return null
        i++
        while (i < content.length && content[i].isWhitespace()) i++
        return extractTypeBeforeAssignment(content, i)
    }

    /** Position of '(' immediately after `invoke`. */
    private fun findInvokeOpenParen(content: String): Int? {
        val sig = Regex("""(?:suspend\s+)?(?:operator\s+)?fun\s+invoke\s*""").find(content) ?: return null
        var i = sig.range.last + 1
        while (i < content.length && content[i].isWhitespace()) i++
        return if (i < content.length && content[i] == '(') i else null
    }

    private fun extractBalancedParenContent(
        content: String,
        openParenIndex: Int,
    ): String? {
        val close = findMatchingCloseParen(content, openParenIndex) ?: return null
        return content.substring(openParenIndex + 1, close)
    }

    /** Match the `)` that closes [openParenIndex] using only `(` / `)` depth (handles `() -> Unit` in params). */
    private fun findMatchingCloseParen(
        content: String,
        openParenIndex: Int,
    ): Int? {
        if (openParenIndex >= content.length || content[openParenIndex] != '(') return null
        var depth = 1
        var i = openParenIndex + 1
        while (i < content.length && depth > 0) {
            when (content[i]) {
                '(' -> depth++
                ')' -> depth--
            }
            i++
        }
        return if (depth == 0) i - 1 else null
    }

    /**
     * Return type text after `:` until `=` (expression body) or `{` (block body) at the top level
     * (not inside `<>`), allowing nested generics.
     */
    private fun extractTypeBeforeAssignment(
        content: String,
        startIndex: Int,
    ): String? {
        var i = startIndex
        var depthAngle = 0
        val typeStart = i
        while (i < content.length) {
            when (content[i]) {
                '<' -> depthAngle++
                '>' -> if (depthAngle > 0) depthAngle--
                '=' -> if (depthAngle == 0) return content.substring(typeStart, i).trim()
                '{' -> if (depthAngle == 0) return content.substring(typeStart, i).trim()
            }
            i++
        }
        return null
    }

    /**
     * Split parameter list on commas that are not inside `()`, `<>`, or `[]`.
     */
    private fun splitTopLevelCommaParams(paramsBlock: String): List<String> {
        val out = mutableListOf<String>()
        var depthParen = 0
        var depthAngle = 0
        var depthBracket = 0
        var start = 0
        var i = 0
        while (i <= paramsBlock.length) {
            val atEnd = i == paramsBlock.length
            val c = if (atEnd) ',' else paramsBlock[i]
            if (!atEnd) {
                when (c) {
                    '(' -> depthParen++
                    ')' -> if (depthParen > 0) depthParen--
                    '<' -> depthAngle++
                    '>' -> if (depthAngle > 0) depthAngle--
                    '[' -> depthBracket++
                    ']' -> if (depthBracket > 0) depthBracket--
                }
            }
            if (atEnd || (c == ',' && depthParen == 0 && depthAngle == 0 && depthBracket == 0)) {
                val part = paramsBlock.substring(start, i).trim()
                if (part.isNotEmpty()) out.add(part)
                start = i + 1
            }
            i++
        }
        return out
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
