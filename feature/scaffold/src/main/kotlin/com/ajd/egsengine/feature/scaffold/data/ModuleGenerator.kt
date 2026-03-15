package com.ajd.egsengine.feature.scaffold.data

import com.ajd.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

class ModuleGenerator {
    private val logger = LoggerFactory.getLogger(ModuleGenerator::class.java)

    data class GeneratedFile(val path: String, val content: String?)

    fun preview(projectRoot: File, template: ModuleTemplate): List<GeneratedFile> {
        val files = mutableListOf<GeneratedFile>()
        val moduleDir = "feature/${template.name}"
        val packagePath = template.packageName.replace('.', '/')
        val sourceBase = "$moduleDir/src/main/kotlin/$packagePath"

        files.add(GeneratedFile("$moduleDir/build.gradle.kts", generateBuildFile(template)))
        files.add(GeneratedFile("$sourceBase/${toPascalCase(template.name)}KoinModule.kt", generateKoinModule(template)))

        for (layer in template.layers) {
            files.add(GeneratedFile("$sourceBase/$layer/.gitkeep", null))
        }

        if (template.hasRes) {
            files.add(GeneratedFile("$moduleDir/src/main/res/values/.gitkeep", null))
        }

        return files
    }

    fun generate(projectRoot: File, template: ModuleTemplate): List<File> {
        val created = mutableListOf<File>()

        for (entry in preview(projectRoot, template)) {
            val file = projectRoot.resolve(entry.path)
            file.parentFile.mkdirs()

            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }

            created.add(file)
            logger.debug("Created: ${entry.path}")
        }

        logger.info("Generated ${created.size} files for module '${template.name}'")
        return created
    }

    private fun generateBuildFile(template: ModuleTemplate): String = buildString {
        appendLine("plugins {")
        if (template.conventionPluginId != null) {
            appendLine("    id(\"${template.conventionPluginId}\")")
        } else {
            appendLine("    id(\"org.jetbrains.kotlin.jvm\")")
        }
        appendLine("}")

        val isAndroid = template.projectType in listOf("ANDROID", "KMP_ANDROID")
        if (isAndroid && template.namespace != null) {
            appendLine()
            appendLine("android {")
            appendLine("    namespace = \"${template.namespace}\"")
            appendLine("}")
        }
    }

    private fun generateKoinModule(template: ModuleTemplate): String {
        val className = toPascalCase(template.name)
        return buildString {
            appendLine("package ${template.packageName}")
            appendLine()
            appendLine("import org.koin.dsl.module")
            appendLine()
            appendLine("val ${toCamelCase(template.name)}KoinModule = module {")
            appendLine("    // TODO: register ${className} dependencies")
            appendLine("}")
            appendLine()
        }
    }

    private fun toPascalCase(name: String): String =
        name.split("-", "_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }

    private fun toCamelCase(name: String): String {
        val pascal = toPascalCase(name)
        return pascal.replaceFirstChar { it.lowercase() }
    }
}
