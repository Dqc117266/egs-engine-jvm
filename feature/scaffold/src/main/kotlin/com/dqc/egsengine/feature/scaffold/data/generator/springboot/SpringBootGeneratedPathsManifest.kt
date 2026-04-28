/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import org.slf4j.LoggerFactory
import java.io.File

data class SpringBootGeneratedManifestDto(
    val version: Int = 1,
    val module: String,
    val table: String?,
    val generatedPaths: List<String>,
)

/** Persists paths written under `feature/<module>/` (for stale cleanup with `--force`). */
class SpringBootGeneratedPathsManifest {

    private val logger = LoggerFactory.getLogger(SpringBootGeneratedPathsManifest::class.java)

    companion object {
        const val FILE_NAME: String = ".egs-generated.json"
    }

    fun read(backendRoot: File, moduleName: String): SpringBootGeneratedManifestDto? {
        val f = moduleRoot(backendRoot, moduleName).resolve(FILE_NAME)
        if (!f.exists()) return null
        return parseJson(f.readText())
    }

    fun write(backendRoot: File, moduleName: String, tableName: String?, paths: Collection<String>) {
        val escapedPaths = paths.sorted().joinToString(",\n") { "    \"" + escapeJson(it) + "\"" }
        val tableJson =
            if (tableName == null) {
                "null"
            } else {
                "\"" + escapeJson(tableName) + "\""
            }
        val body =
            """
            {
              "version": 1,
              "module": "${escapeJson(moduleName)}",
              "table": $tableJson,
              "generatedPaths": [
            $escapedPaths
              ]
            }
            """.trimIndent() + "\n"
        val f = moduleRoot(backendRoot, moduleName).resolve(FILE_NAME)
        f.parentFile?.mkdirs()
        f.writeText(body)
    }

    fun deleteTrackedFiles(backendRoot: File, dto: SpringBootGeneratedManifestDto) {
        val root = backendRoot.canonicalPath
        for (p in dto.generatedPaths) {
            val target = backendRoot.resolve(p)
            try {
                val normalized = target.canonicalPath
                if (!normalized.startsWith(root)) continue
                if (target.isFile) target.delete()
            } catch (e: Exception) {
                logger.debug("Could not delete {}", target, e)
            }
        }
    }

    private fun parseJson(text: String): SpringBootGeneratedManifestDto? =
        try {
            val pathsMatch = Regex("\"generatedPaths\"\\s*:\\s*\\[([^]]*)]", RegexOption.DOT_MATCHES_ALL).find(text)
                ?: return null
            val inner = pathsMatch.groupValues[1]
            val paths = Regex("\"([^\"]*)\"").findAll(inner).map { it.groupValues[1] }.toList()
            val module = Regex("\"module\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.getOrNull(1) ?: return null
            val rawTable = Regex("\"table\"\\s*:\\s*(null|\"([^\"]*)\")").find(text)
            val table = when (rawTable?.groupValues?.getOrNull(1)) {
                null, "null" -> null
                else -> rawTable.groupValues.getOrNull(2)
            }
            SpringBootGeneratedManifestDto(module = module, table = table, generatedPaths = paths)
        } catch (_: Exception) {
            null
        }

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun moduleRoot(backendRoot: File, moduleName: String): File =
        backendRoot.resolve("feature/$moduleName")
}
