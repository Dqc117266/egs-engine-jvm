/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import org.slf4j.LoggerFactory
import java.io.File

data class SpringBootGeneratedManifestDto(
    val version: Int,
    val module: String,
    val table: String?,
    val generatedPaths: List<String>,
    val codegen: BackendCodegenManifest?,
)

/** Persists paths under `feature/<module>/` and optional `codegen` for admin tooling. */
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

    fun write(
        backendRoot: File,
        moduleName: String,
        tableName: String?,
        paths: Collection<String>,
        codegen: BackendCodegenManifest?,
    ) {
        val escapedPaths = paths.sorted().joinToString(",\n") { "    \"" + escapeJson(it) + "\"" }
        val tableJson =
            if (tableName == null) {
                "null"
            } else {
                "\"" + escapeJson(tableName) + "\""
            }
        val fileVersion = if (codegen != null) 2 else 1
        val codegenBlock =
            if (codegen == null) {
                ""
            } else {
                ",\n  \"codegen\": ${codegenManifestToJson(codegen)}"
            }
        val body =
            """
            {
              "version": $fileVersion,
              "module": "${escapeJson(moduleName)}",
              "table": $tableJson,
              "generatedPaths": [
            $escapedPaths
              ]$codegenBlock
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

    fun readCodegenOnly(backendRoot: File, backendModuleName: String): BackendCodegenManifest? =
        read(backendRoot, backendModuleName)?.codegen

    private fun codegenManifestToJson(m: BackendCodegenManifest): String {
        val cols =
            m.columns.joinToString(separator = ",", prefix = "[", postfix = "]") { c ->
                "{\"kotlinName\":\"${escapeJson(c.kotlinName)}\",\"kotlinType\":\"${escapeJson(c.kotlinType)}\",\"tsType\":\"${escapeJson(c.tsType)}\",\"nullable\":${c.nullable},\"isPk\":${c.isPk},\"inBusinessForm\":${c.inBusinessForm}}"
            }
        return "{" +
            "\"schemaVersion\":${m.schemaVersion}," +
            "\"entityPascal\":\"${escapeJson(m.entityPascal)}\"," +
            "\"entityCamel\":\"${escapeJson(m.entityCamel)}\"," +
            "\"restPath\":\"${escapeJson(m.restPath)}\"," +
            "\"tableSqlName\":\"${escapeJson(m.tableSqlName)}\"," +
            "\"backendModuleName\":\"${escapeJson(m.backendModuleName)}\"," +
            "\"basePackage\":\"${escapeJson(m.basePackage)}\"," +
            "\"pkField\":\"${escapeJson(m.pkField)}\"," +
            "\"pkTsType\":\"${escapeJson(m.pkTsType)}\"," +
            "\"columns\":$cols" +
            "}"
    }

    private fun parseJson(text: String): SpringBootGeneratedManifestDto? =
        try {
            val pathsMatch =
                Regex("\"generatedPaths\"\\s*:\\s*\\[([^]]*)]", RegexOption.DOT_MATCHES_ALL).find(text)
                    ?: return null
            val inner = pathsMatch.groupValues[1]
            val paths = Regex("\"([^\"]*)\"").findAll(inner).map { it.groupValues[1] }.toList()
            val module = Regex("\"module\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.getOrNull(1) ?: return null
            val rawTable = Regex("\"table\"\\s*:\\s*(null|\"([^\"]*)\")").find(text)
            val table = when (rawTable?.groupValues?.getOrNull(1)) {
                null, "null" -> null
                else -> rawTable.groupValues.getOrNull(2)
            }
            val verMatch = Regex("\"version\"\\s*:\\s*([0-9]+)").find(text)
            val version = verMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            val codegen =
                if (!text.contains("\"codegen\"")) {
                    null
                } else {
                    parseCodegenEmbedded(text)
                }
            SpringBootGeneratedManifestDto(
                version = version,
                module = module,
                table = table,
                generatedPaths = paths,
                codegen = codegen,
            )
        } catch (_: Exception) {
            null
        }

    private fun parseCodegenEmbedded(full: String): BackendCodegenManifest? {
        val startIdx = full.indexOf("\"codegen\"")
        if (startIdx < 0) return null
        val braceStart = full.indexOf('{', startIdx)
        if (braceStart < 0) return null
        var depth = 0
        var i = braceStart
        while (i < full.length) {
            when (full[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val objJson = full.substring(braceStart, i + 1)
                        return parseCodegenObjectEmbedded(objJson)
                    }
                }
            }
            i++
        }
        return null
    }

    private fun parseCodegenObjectEmbedded(json: String): BackendCodegenManifest? {
        fun str(key: String): String? =
            Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)

        fun intVal(key: String): Int =
            Regex("\"$key\"\\s*:\\s*([0-9]+)").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 1

        val entityPascal = str("entityPascal") ?: return null
        val columns = extractColumnsFromCodegenJson(json)
        return BackendCodegenManifest(
            schemaVersion = intVal("schemaVersion"),
            entityPascal = entityPascal,
            entityCamel = str("entityCamel") ?: "",
            restPath = str("restPath") ?: "",
            tableSqlName = str("tableSqlName") ?: "",
            backendModuleName = str("backendModuleName") ?: "",
            basePackage = str("basePackage") ?: "",
            pkField = str("pkField") ?: "id",
            pkTsType = str("pkTsType") ?: "number",
            columns = columns,
        )
    }

    private fun extractColumnsFromCodegenJson(json: String): List<BackendCodegenManifestColumn> {
        val cols = mutableListOf<BackendCodegenManifestColumn>()
        val itemRe =
            Regex(
                """\{"kotlinName":"([^"]*)","kotlinType":"([^"]*)","tsType":"([^"]*)","nullable":(true|false),"isPk":(true|false),"inBusinessForm":(true|false)(?:,"formControl":"([^"]*)")?}""",
            )
        itemRe.findAll(json).forEach { m ->
            val kotlinName = m.groupValues[1]
            val kotlinType = m.groupValues[2]
            val tsType = m.groupValues[3]
            val formControlStr = m.groupValues[7]
            val formControl = if (formControlStr.isNotEmpty()) {
                try { FormControl.valueOf(formControlStr) } catch (_: Exception) { FormControl.INPUT }
            } else {
                FormControl.infer(kotlinName, kotlinType, tsType)
            }
            cols.add(
                BackendCodegenManifestColumn(
                    kotlinName = kotlinName,
                    kotlinType = kotlinType,
                    tsType = tsType,
                    nullable = m.groupValues[4] == "true",
                    isPk = m.groupValues[5] == "true",
                    inBusinessForm = m.groupValues[6] == "true",
                    formControl = formControl,
                ),
            )
        }
        return cols
    }

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun moduleRoot(backendRoot: File, moduleName: String): File =
        backendRoot.resolve("feature/$moduleName")
}
