/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.android.AndroidDbOnlyRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpGeneratedDomainModuleIo
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

class SwaggerCodeGenerator(
    private val renderer: SwaggerTemplateRenderer,
    private val androidCombinedRepositoryGenerator: AndroidCombinedRepositoryGenerator,
    private val androidDbOnlyRepositoryImplGenerator: AndroidDbOnlyRepositoryImplGenerator,
) {
    private val logger = LoggerFactory.getLogger(SwaggerCodeGenerator::class.java)

    fun generateToCommon(
        template: ModuleTemplate,
        spec: SwaggerSpec,
        projectRoot: File? = null,
    ): List<GeneratedFile> = generate(template, spec, projectRoot).map { GeneratedFile(it.path, it.content) }

    fun generate(
        template: ModuleTemplate,
        spec: SwaggerSpec,
        projectRoot: File? = null,
    ): List<ModuleGenerator.GeneratedFile> {
        val moduleDir = "feature/${template.name}"
        val files = mutableListOf<ModuleGenerator.GeneratedFile>()
        val ctx = AndroidSwaggerGeneratorContext(template)

        val (wrapperSchemas, dataSchemas) = spec.schemas.partition { SwaggerTypeMapping.isCommonResultWrapper(it) }
        val wrapperUnwrapMap =
            wrapperSchemas.associate { schema ->
                schema.name to schema.properties.firstOrNull { it.originalName == "data" }?.type
            }
        val requestSchemaNames = SwaggerTypeMapping.collectRequestSchemaNames(spec)

        for (schema in dataSchemas) {
            files.addSwagger(
                moduleDir,
                ctx.dataModelPackage,
                ctx.dataModelName(schema.name),
                renderer.renderDataModel(schema, ctx, schema.name in requestSchemaNames),
            )
            files.addSwagger(
                moduleDir,
                ctx.domainModelPackage,
                ctx.domainModelName(schema.name),
                renderer.renderDomainModel(schema, ctx),
            )
        }

        val adjustedSpec =
            spec.copy(
                operations =
                spec.operations.map { op ->
                    op.copy(
                        params = op.params.filter { it.location.lowercase() != "header" },
                        responseBody = SwaggerTypeMapping.unwrapResponseBody(op.responseBody, wrapperUnwrapMap),
                    )
                },
            )

        files.addSwagger(moduleDir, ctx.servicePackage, ctx.serviceName, renderer.renderServiceInterface(adjustedSpec, ctx))
        files.addSwagger(
            moduleDir,
            ctx.domainRepositoryPackage,
            ctx.apiRepositoryName,
            renderer.renderRepositoryInterface(adjustedSpec, ctx),
        )
        files.addSwagger(
            moduleDir,
            ctx.dataRepositoryPackage,
            ctx.apiRepositorySupportName,
            renderer.renderGeneratedRepositorySupport(adjustedSpec, ctx),
        )
        val renderedDataModule = renderer.renderGeneratedDataModule(ctx)
        val mergedDataModule =
            projectRoot?.let { root ->
                val existingPath =
                    root.resolve(
                        "$moduleDir/src/main/kotlin/${ctx.generateDiPackage.replace('.', '/')}/GeneratedDataModule.kt",
                    )
                val existing = if (existingPath.exists()) existingPath.readText() else null
                KmpGeneratedDomainModuleIo.mergeGeneratedDataModulePreservingDatabaseBlock(existing, renderedDataModule)
            } ?: renderedDataModule
        files.addSwagger(moduleDir, ctx.generateDiPackage, "GeneratedDataModule", mergedDataModule)
        val preservedDb =
            projectRoot
                ?.let { root ->
                    KmpGeneratedDomainModuleIo.extractDbUseCaseClassNames(root, template.name, template, kotlinSourceSet = "main")
                }.orEmpty()
        val preservedPrefs =
            projectRoot
                ?.let { root ->
                    KmpGeneratedDomainModuleIo.extractPrefsUseCaseClassNames(root, template.name, template, kotlinSourceSet = "main")
                }.orEmpty()
        files.addSwagger(
            moduleDir,
            ctx.generateDiPackage,
            "GeneratedDomainModule",
            renderer.renderGeneratedDomainModule(
                adjustedSpec,
                ctx,
                preservedDbUseCaseClassNames = preservedDb,
                preservedPrefsUseCaseClassNames = preservedPrefs,
                projectRoot = projectRoot,
            ),
        )

        adjustedSpec.operations.forEach { op ->
            val useCaseName = "${op.operationId.toSafePascal()}UseCase"
            files.addSwagger(
                moduleDir,
                ctx.domainUseCasePackage,
                useCaseName,
                renderer.renderUseCase(op, ctx),
            )
        }

        val slices =
            projectRoot?.let { root ->
                androidCombinedRepositoryGenerator.detectSlices(root, template.name, template)
            }
        val includeDb = slices?.hasDb ?: false
        val includePrefs = slices?.hasPrefs ?: false
        androidCombinedRepositoryGenerator
            .generate(
                template = template,
                subProjectRoot = projectRoot,
                includeApi = true,
                includeDb = includeDb,
                includePrefs = includePrefs,
            )?.let { combined ->
                files.add(ModuleGenerator.GeneratedFile(combined.path, combined.content))
            }
        androidDbOnlyRepositoryImplGenerator
            .generateOrMerge(
                template = template,
                subProjectRoot = projectRoot,
                includeApi = true,
                includeDb = includeDb,
                includePrefs = includePrefs,
            )?.let { impl ->
                files.add(ModuleGenerator.GeneratedFile(impl.path, impl.content))
            }

        logger.info("Generated ${files.size} swagger scaffold files for module ${template.name}")
        return files
    }

    private fun MutableList<ModuleGenerator.GeneratedFile>.addSwagger(
        moduleDir: String,
        packageName: String,
        fileNameWithoutExtension: String,
        content: String,
    ) {
        val pkgPath = packageName.replace('.', '/')
        add(ModuleGenerator.GeneratedFile("$moduleDir/src/main/kotlin/$pkgPath/$fileNameWithoutExtension.kt", content))
    }
}
