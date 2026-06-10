package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Legacy orchestrator kept for backward compatibility with single-project (EgsConfig) flows.
 * Delegates to [AndroidModuleGenerator.previewFromTemplate].
 */
class ModuleGenerator(
    private val androidModuleGenerator: AndroidModuleGenerator,
) {
    private val logger = LoggerFactory.getLogger(ModuleGenerator::class.java)

    data class GeneratedFile(
        val path: String,
        val content: String?,
    )

    fun preview(
        projectRoot: File,
        template: ModuleTemplate,
    ): List<GeneratedFile> = androidModuleGenerator.previewFromTemplate(template, projectRoot).map {
        GeneratedFile(it.path, it.content)
    }

    fun generate(
        projectRoot: File,
        template: ModuleTemplate,
    ): List<File> {
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
}
