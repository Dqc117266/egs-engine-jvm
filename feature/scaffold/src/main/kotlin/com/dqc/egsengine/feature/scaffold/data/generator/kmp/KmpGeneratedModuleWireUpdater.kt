/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.domain.toModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Idempotently wires `generatedDataModule` / `generatedDomainModule` into the feature root Koin module
 * (`<Pascal>Module.kt`), matching egs-kmp-template style.
 */
class KmpGeneratedModuleWireUpdater {

    private val logger = LoggerFactory.getLogger(KmpGeneratedModuleWireUpdater::class.java)

    fun wireIfNeeded(
        subProjectRoot: File,
        moduleName: String,
        config: SubProjectConfig,
    ) {
        if (config.platform !in setOf(Platform.KMP, Platform.KMP_ANDROID)) return

        val template = config.toModuleTemplate(moduleName)
        val packageName = template.packageName
        val pascal = moduleName.kmpModulePascalCase()
        val pkgPath = packageName.replace('.', '/')
        val rootFile = subProjectRoot.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/${pascal}Module.kt",
        )
        if (!rootFile.exists()) {
            logger.debug("KMP root module not found (skip wire): {}", rootFile.path)
            return
        }

        val original = rootFile.readText()
        var text = original
        if (text.contains("generatedDataModule,") && text.contains("generatedDomainModule,")) {
            return
        }

        val importData = "import $packageName.generate.generatedDataModule"
        val importDomain = "import $packageName.generate.generatedDomainModule"
        if (!text.contains(importData)) {
            text = insertAfterPackage(text, "$importData\n$importDomain\n")
        }

        val defaultListBlock = """
    dataModule,
    domainModule,
    presentationModule,
""".trimEnd()
        val wiredListBlock = """
    generatedDataModule,
    dataModule,
    generatedDomainModule,
    domainModule,
    presentationModule,
""".trimEnd()
        val replaced = if (text.contains(defaultListBlock)) {
            text.replaceFirst(defaultListBlock, wiredListBlock)
        } else {
            text
        }
        if (replaced == original) {
            logger.warn(
                "Could not auto-wire generated Koin modules into {} — add generatedDataModule / generatedDomainModule manually.",
                rootFile.path,
            )
            return
        }
        rootFile.writeText(replaced)
        logger.info("Wired generatedDataModule / generatedDomainModule into {}", rootFile.path)
    }

    private fun insertAfterPackage(text: String, insertion: String): String {
        val match = Regex("^package\\s+[^\\s]+", RegexOption.MULTILINE).find(text) ?: return text
        val insertAt = match.range.last + 1
        return text.substring(0, insertAt) + "\n\n" + insertion + text.substring(insertAt)
    }
}

private fun String.kmpModulePascalCase(): String =
    split("-", "_").joinToString("") { part ->
        part.replaceFirstChar { c -> c.uppercase() }
    }.ifBlank { "Feature" }
