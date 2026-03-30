package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.config.WorkspaceConfigResolver
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.SpringBootCrudGenerator
import org.slf4j.LoggerFactory
import java.io.File

/**
 * DDL -> backend CRUD code orchestrator.
 * Reads workspace config, parses DDL, delegates to [SpringBootCrudGenerator].
 *
 * Skeleton -- concrete orchestration to be implemented with the DDL parser.
 */
class EntityScaffolder(
    private val workspaceResolver: WorkspaceConfigResolver,
    private val ddlParser: DdlParser,
    private val crudGenerator: SpringBootCrudGenerator,
) {
    private val logger = LoggerFactory.getLogger(EntityScaffolder::class.java)

    fun scaffold(
        projectRoot: File,
        moduleName: String,
        ddlSql: String,
        dryRun: Boolean = false,
    ): EntityScaffoldResult {
        logger.info("EntityScaffolder.scaffold() called for module '{}' -- not yet implemented", moduleName)
        TODO("Entity scaffolding not yet implemented")
    }

    fun scaffoldFromFile(
        projectRoot: File,
        moduleName: String,
        ddlFile: File,
        dryRun: Boolean = false,
    ): EntityScaffoldResult {
        require(ddlFile.exists()) { "DDL file not found: ${ddlFile.absolutePath}" }
        return scaffold(projectRoot, moduleName, ddlFile.readText(), dryRun)
    }

    data class EntityScaffoldResult(
        val moduleName: String,
        val tables: List<String>,
        val files: List<GeneratedFile>,
        val dryRun: Boolean,
    )
}
