/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Delegates Retrofit [GeneratedXRepositorySupport] and Room [GeneratedXDbRepositorySupport] for Android Swagger (`XRepository`, not `XApiRepository`).
 */
class AndroidApiDbRepositoryImplGenerator {

    private val logger = LoggerFactory.getLogger(AndroidApiDbRepositoryImplGenerator::class.java)

    fun generateOrMerge(
        template: ModuleTemplate,
        subProjectRoot: File?,
    ): GeneratedFile? {
        val pascal = SqlNaming.moduleNameToPascal(template.name)
        val pkg = template.packageName
        val moduleDir = "feature/${template.name}"
        val pkgPath = pkg.replace('.', '/')
        val path = "$moduleDir/src/main/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt"
        val content = renderContent(pascal, pkg)

        if (subProjectRoot != null) {
            val file = subProjectRoot.resolve(path)
            if (file.exists()) {
                val existing = file.readText()
                if (existing.contains(AndroidRepositoryCodegen.FREEZE_MARKER)) {
                    logger.debug("Skip Android API+DB RepositoryImpl (frozen): {}", file.path)
                    return null
                }
            }
        }

        return GeneratedFile(path, content)
    }

    private fun renderContent(pascal: String, pkg: String): String =
        """
        /*
         * Hand-written repository: delegates to generated API and DB support classes.
         * egs-codegen: scaffold-repository-impl-delegation
         * Add ${AndroidRepositoryCodegen.FREEZE_MARKER} on its own line to prevent overwrites.
         */
        package $pkg.data.repository

        import $pkg.generate.data.repository.Generated${pascal}DbRepositorySupport
        import $pkg.generate.data.repository.Generated${pascal}RepositorySupport
        import $pkg.generate.domain.repository.${pascal}DbRepository
        import $pkg.generate.domain.repository.${pascal}Repository

        internal class ${pascal}RepositoryImpl(
            apiSupport: Generated${pascal}RepositorySupport,
            dbSupport: Generated${pascal}DbRepositorySupport,
        ) : ${pascal}Repository by apiSupport,
            ${pascal}DbRepository by dbSupport
        """.trimIndent() + "\n"
}
