package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.KotlinFileGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.XmlTemplateGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.template.toFixedString
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.squareup.kotlinpoet.FileSpec
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Legacy orchestrator kept for backward compatibility with single-project (EgsConfig) flows.
 * New multi-platform code should use [AndroidModuleGenerator] / platform generators directly.
 */
class ModuleGenerator {
    private val logger = LoggerFactory.getLogger(ModuleGenerator::class.java)

    data class GeneratedFile(val path: String, val content: String?)

    fun preview(projectRoot: File, template: ModuleTemplate): List<GeneratedFile> {
        val kotlinGen = KotlinFileGenerator(template)
        val xmlGen = XmlTemplateGenerator(template)
        val files = mutableListOf<GeneratedFile>()
        val moduleDir = "feature/${template.name}"
        val isAndroid = template.isAndroid

        files.add(GeneratedFile("$moduleDir/build.gradle.kts", generateBuildFile(template)))

        files.addKt(moduleDir, kotlinGen.generateRootKoinModule())
        files.addKt(moduleDir, kotlinGen.generateDataModule())
        files.addKt(moduleDir, kotlinGen.generateDomainModule())
        files.addKt(moduleDir, kotlinGen.generatePresentationModule())
        files.addKt(moduleDir, kotlinGen.generateRepositoryInterface())
        files.addKt(moduleDir, kotlinGen.generateRepositoryImpl())
        files.addKt(moduleDir, kotlinGen.generateViewModel())

        if (isAndroid) {
            kotlinGen.generateContract()?.let { files.addKt(moduleDir, it) }
            kotlinGen.generateNavigationRoute()?.let { files.addKt(moduleDir, it) }

            files.add(
                GeneratedFile(
                    "$moduleDir/src/main/AndroidManifest.xml",
                    xmlGen.generateAndroidManifest(),
                ),
            )
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
            logger.debug("Created: {}", entry.path)
        }

        logger.info("Generated {} files for module '{}'", created.size, template.name)
        return created
    }

    private fun MutableList<GeneratedFile>.addKt(moduleDir: String, fileSpec: FileSpec) {
        val pkgPath = fileSpec.packageName.replace('.', '/')
        val path = "$moduleDir/src/main/kotlin/$pkgPath/${fileSpec.name}.kt"
        add(GeneratedFile(path, fileSpec.toFixedString()))
    }

    private fun generateBuildFile(template: ModuleTemplate): String = buildString {
        val isAndroid = template.isAndroid

        appendLine("plugins {")
        if (template.conventionPluginId != null) {
            appendLine("    id(\"${template.conventionPluginId}\")")
        } else {
            appendLine("    id(\"org.jetbrains.kotlin.jvm\")")
        }
        appendLine("}")

        if (isAndroid && template.namespace != null) {
            appendLine()
            appendLine("android {")
            appendLine("    namespace = \"${template.namespace}\"")
            appendLine("}")
        }
    }
}
