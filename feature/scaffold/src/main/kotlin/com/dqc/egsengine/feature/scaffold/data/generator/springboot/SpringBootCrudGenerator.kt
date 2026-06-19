/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SchemaTraitInferrer
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootCodegenModelBuilder
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootCrudTemplateRenderer
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import org.slf4j.LoggerFactory
import java.io.File

class SpringBootCrudGenerator(
    private val templateEngine: TemplateEngine,
) {
    private val logger = LoggerFactory.getLogger(SpringBootCrudGenerator::class.java)
    private val inferrer = SchemaTraitInferrer()
    private val builder = SpringBootCodegenModelBuilder(inferrer)
    private val renderer = SpringBootCrudTemplateRenderer(templateEngine)

    fun generate(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions = SpringBootOpinionatedOptions(),
        projectRoot: File?,
    ): List<GeneratedFile> {
        val model = builder.build(table, moduleName, config, options)
        val files = renderer.renderGeneratedSources(model, projectRoot)
        logger.info(
            "SpringBootCrudGenerator: {} file(s) for table '{}' module '{}'",
            files.size,
            table.tableName,
            moduleName,
        )
        return files
    }

    fun preview(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions,
        projectRoot: File?,
    ): List<GeneratedFile> = generate(table, moduleName, config, options, projectRoot)

    fun buildCodegenManifest(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
        options: SpringBootOpinionatedOptions,
    ): com.dqc.egsengine.feature.scaffold.data.generator.springboot.BackendCodegenManifest = builder.buildBackendCodegenManifest(table, moduleName, config, options)
}
