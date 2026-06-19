/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpCombinedRepositoryGenerator
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpGeneratedDomainModuleIo
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.KmpRepositoryImplGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Swagger codegen for KMP `commonMain` under `feature/<module>/.../generate/`.
 */
class KmpSwaggerCodeGenerator(
    private val renderer: KmpSwaggerTemplateRenderer,
    private val combinedRepositoryGenerator: KmpCombinedRepositoryGenerator,
    private val repositoryImplGenerator: KmpRepositoryImplGenerator,
) {
    private val logger = LoggerFactory.getLogger(KmpSwaggerCodeGenerator::class.java)
    private val pagingInferrer = SwaggerPagingInferrer()

    fun generateToCommon(
        template: ModuleTemplate,
        spec: SwaggerSpec,
        projectRoot: File? = null,
    ): List<GeneratedFile> = generate(template, spec, projectRoot).map { GeneratedFile(it.path, it.content) }

    /**
     * Applies the same wrapper unwrap and header filtering as [generate].
     */
    fun adjustSpecForKmp(spec: SwaggerSpec): SwaggerSpec {
        val (wrapperSchemas, _) = spec.schemas.partition { SwaggerTypeMapping.isCommonResultWrapper(it) }
        val wrapperUnwrapMap =
            wrapperSchemas.associate { schema ->
                schema.name to schema.properties.firstOrNull { it.originalName == "data" }?.type
            }
        return spec.copy(
            operations =
            spec.operations.map { op ->
                op.copy(
                    params = op.params.filter { it.location.lowercase() != "header" },
                    responseBody = SwaggerTypeMapping.unwrapResponseBody(op.responseBody, wrapperUnwrapMap),
                )
            },
        )
    }

    fun generate(
        template: ModuleTemplate,
        spec: SwaggerSpec,
        projectRoot: File? = null,
    ): List<ModuleGenerator.GeneratedFile> {
        val moduleDir = "feature/${template.name}"
        val files = mutableListOf<ModuleGenerator.GeneratedFile>()
        val ctx = KmpSwaggerGeneratorContext(template)

        val adjustedSpec = adjustSpecForKmp(spec)
        val specForGen = pagingInferrer.enrich(adjustedSpec)
        val (wrapperSchemas, dataSchemas) = spec.schemas.partition { SwaggerTypeMapping.isCommonResultWrapper(it) }
        val wrapperUnwrapMap =
            wrapperSchemas.associate { schema ->
                schema.name to schema.properties.firstOrNull { it.originalName == "data" }?.type
            }
        val requestSchemaNames = SwaggerTypeMapping.collectRequestSchemaNames(spec)

        for (schema in dataSchemas) {
            files.addCommonMain(
                moduleDir,
                ctx.dataModelPackage,
                ctx.dataModelName(schema.name),
                renderer.renderDataModel(schema, ctx, schema.name in requestSchemaNames),
            )
            files.addCommonMain(
                moduleDir,
                ctx.domainModelPackage,
                ctx.domainModelName(schema.name),
                renderer.renderDomainModel(schema, ctx),
            )
        }

        files.addCommonMain(
            moduleDir,
            ctx.servicePackage,
            ctx.serviceName,
            renderer.renderKtorfitServiceInterface(specForGen, ctx),
        )
        files.addCommonMain(
            moduleDir,
            ctx.domainRepositoryPackage,
            ctx.apiRepositoryName,
            renderer.renderRepositoryInterface(specForGen, ctx),
        )
        files.addCommonMain(
            moduleDir,
            ctx.dataRepositoryPackage,
            ctx.apiRepositorySupportName,
            renderer.renderApiRepositorySupport(specForGen, ctx),
        )
        val renderedDataModule = renderer.renderGeneratedDataModule(ctx)
        val mergedDataModule =
            projectRoot?.let { root ->
                val existingPath =
                    root.resolve(
                        "$moduleDir/src/commonMain/kotlin/${ctx.generateDiPackage.replace('.', '/')}/GeneratedDataModule.kt",
                    )
                val existing = if (existingPath.exists()) existingPath.readText() else null
                KmpGeneratedDomainModuleIo.mergeGeneratedDataModulePreservingDatabaseBlock(existing, renderedDataModule)
            } ?: renderedDataModule
        files.addCommonMain(
            moduleDir,
            ctx.generateDiPackage,
            "GeneratedDataModule",
            mergedDataModule,
        )
        val preservedDb =
            projectRoot
                ?.let { root ->
                    KmpGeneratedDomainModuleIo.extractDbUseCaseClassNames(root, template.name, template)
                }.orEmpty()
        val preservedPrefs =
            projectRoot
                ?.let { root ->
                    KmpGeneratedDomainModuleIo.extractPrefsUseCaseClassNames(root, template.name, template)
                }.orEmpty()
        files.addCommonMain(
            moduleDir,
            ctx.generateDiPackage,
            "GeneratedDomainModule",
            renderer.renderGeneratedDomainModule(specForGen, ctx, preservedDb, preservedPrefs),
        )

        specForGen.operations.forEach { op ->
            val useCaseName = "${op.operationId.toSafePascal()}UseCase"
            files.addCommonMain(
                moduleDir,
                ctx.domainUseCasePackage,
                useCaseName,
                renderer.renderUseCase(op, ctx),
            )
        }

        val slices =
            projectRoot?.let { root ->
                combinedRepositoryGenerator.detectSlices(root, template.name, template)
            }
        val includeDb = slices?.hasDb ?: false
        val includePrefs = slices?.hasPrefs ?: false
        combinedRepositoryGenerator
            .generate(
                template = template,
                subProjectRoot = projectRoot,
                includeApi = true,
                includeDb = includeDb,
                includePrefs = includePrefs,
            )?.let { files.add(ModuleGenerator.GeneratedFile(it.path, it.content)) }

        repositoryImplGenerator
            .generateOrMerge(
                template = template,
                subProjectRoot = projectRoot,
                includeApi = true,
                includeDb = includeDb,
                includePrefs = includePrefs,
            )?.let { files.add(ModuleGenerator.GeneratedFile(it.path, it.content)) }

        logger.info("Generated ${files.size} KMP swagger scaffold files for module ${template.name}")
        return files
    }

    private fun MutableList<ModuleGenerator.GeneratedFile>.addCommonMain(
        moduleDir: String,
        packageName: String,
        fileNameWithoutExtension: String,
        content: String,
    ) {
        val pkgPath = packageName.replace('.', '/')
        add(
            ModuleGenerator.GeneratedFile(
                "$moduleDir/src/commonMain/kotlin/$pkgPath/$fileNameWithoutExtension.kt",
                content,
            ),
        )
    }
}
