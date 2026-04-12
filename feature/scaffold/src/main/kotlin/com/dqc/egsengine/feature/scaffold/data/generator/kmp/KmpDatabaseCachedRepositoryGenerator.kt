/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.ddl.SqlNaming
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerGeneratorContext
import com.dqc.egsengine.feature.scaffold.data.swagger.KmpSwaggerTemplateRenderer
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerOperation
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerSpec
import com.dqc.egsengine.feature.scaffold.data.swagger.SwaggerType
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.feature.scaffold.data.swagger.toSafeIdentifier
import java.io.File

/**
 * Mode C: rewrites [Generated*RepositorySupport] with cache-aside for GET operations when
 * response models map to DDL tables (see [KmpDatabaseEntityMapperGenerator]).
 */
class KmpDatabaseCachedRepositoryGenerator(
    private val kmpSwaggerTemplateRenderer: KmpSwaggerTemplateRenderer,
    private val entityMapperGenerator: KmpDatabaseEntityMapperGenerator,
) {

    fun generateCachedRepositorySupport(
        template: ModuleTemplate,
        tables: List<TableSchema>,
        adjustedSpec: SwaggerSpec,
        projectRoot: File?,
    ): GeneratedFile? {
        val ctx = KmpSwaggerGeneratorContext(template)
        if (!ctx.hasResultWrappers()) {
            return null
        }

        val domainToRow = entityMapperGenerator.buildDomainSchemaToRow(tables, adjustedSpec)
        if (domainToRow.isEmpty()) return null

        val modulePascal = SqlNaming.moduleNameToPascal(template.name)
        val dataSourceClass = "${modulePascal}DatabaseDataSource"
        val pkg = template.packageName
        val mapperPackage = "$pkg.generate.data.datasource.database.mapper"
        val extraImports = setOf("$mapperPackage.*")

        val statementOverride: (SwaggerOperation, String) -> String = { op, defaultStmt ->
            cachedStatementOrDefault(op, defaultStmt, adjustedSpec, ctx, domainToRow)
        }

        val content = kmpSwaggerTemplateRenderer.renderGeneratedRepositorySupport(
            spec = adjustedSpec,
            ctx = ctx,
            projectRoot = projectRoot,
            includeDbDataSource = true,
            dbDataSourceClass = dataSourceClass,
            statementOverride = statementOverride,
            extraImports = extraImports,
        )

        val moduleDir = "feature/${template.name}"
        val dataRepoPkg = ctx.dataRepositoryPackage
        val pkgPath = dataRepoPkg.replace('.', '/')
        return GeneratedFile(
            "$moduleDir/src/commonMain/kotlin/$pkgPath/${ctx.repositoryImplName}.kt",
            content,
        )
    }

    private fun cachedStatementOrDefault(
        op: SwaggerOperation,
        defaultStmt: String,
        spec: SwaggerSpec,
        ctx: KmpSwaggerGeneratorContext,
        domainToRow: Map<String, KmpDatabaseTableRow>,
    ): String {
        if (op.method.uppercase() != "GET") return defaultStmt
        val body = op.responseBody ?: return defaultStmt

        when (body) {
            is SwaggerType.ModelRef -> {
                domainToRow[body.name]?.let { return singleModelCacheStatement(op, it, ctx) }
                return tryPageResult(body.name, op, spec, ctx, domainToRow, defaultStmt)
            }
            else -> return defaultStmt
        }
    }

    private fun tryPageResult(
        modelName: String,
        op: SwaggerOperation,
        spec: SwaggerSpec,
        ctx: KmpSwaggerGeneratorContext,
        domainToRow: Map<String, KmpDatabaseTableRow>,
        defaultStmt: String,
    ): String {
        val pageSchema = spec.schemas.find { it.name == modelName } ?: return defaultStmt
        val elementName = KmpDatabaseEntityMapperGenerator.pageListElementSchemaName(pageSchema) ?: return defaultStmt
        val row = domainToRow[elementName] ?: return defaultStmt
        return pageListCacheStatement(op, row, pageSchema.name, ctx)
    }

    private fun callArgs(op: SwaggerOperation): String {
        val parts = mutableListOf<String>()
        op.params.forEach { parts.add(it.name.toSafeIdentifier()) }
        if (op.requestBody != null) parts.add("body.toData()")
        return parts.joinToString(", ")
    }

    private fun singleModelCacheStatement(
        op: SwaggerOperation,
        row: KmpDatabaseTableRow,
        ctx: KmpSwaggerGeneratorContext,
    ): String {
        val serviceCall = "service.${op.operationId}(${callArgs(op)})"
        val mapperExpr = ctx.repositoryResponseMapExpression(op.responseBody, "it")
            ?: return kmpSwaggerTemplateRenderer.defaultRepositorySupportStatement(op, ctx)
        val prefix = row.prefixPascal
        val idParam = op.params.firstOrNull { it.name.equals("id", ignoreCase = true) }
            ?: op.params.firstOrNull()
        val idName = idParam?.name?.toSafeIdentifier() ?: "id"
        return """
            return try {
                val result = $serviceCall.toResult { $mapperExpr }
                if (result is Result.Success) {
                    dbDataSource.insert$prefix(result.value.toEntity())
                }
                result
            } catch (e: Exception) {
                val cached = dbDataSource.get${prefix}ById($idName)
                if (cached != null) {
                    Result.Success(cached.toDomain())
                } else {
                    Result.Failure(e)
                }
            }
        """.trimIndent()
    }

    private fun pageListCacheStatement(
        op: SwaggerOperation,
        row: KmpDatabaseTableRow,
        pageSchemaName: String,
        ctx: KmpSwaggerGeneratorContext,
    ): String {
        val serviceCall = "service.${op.operationId}(${callArgs(op)})"
        val mapperExpr = ctx.repositoryResponseMapExpression(op.responseBody, "it")
            ?: return kmpSwaggerTemplateRenderer.defaultRepositorySupportStatement(op, ctx)
        val prefix = row.prefixPascal
        val pageType = ctx.domainModelName(pageSchemaName)
        return """
            return try {
                val result = $serviceCall.toResult { $mapperExpr }
                if (result is Result.Success) {
                    dbDataSource.deleteAll$prefix()
                    dbDataSource.insertAll$prefix(result.value.list.map { it.toEntity() })
                }
                result
            } catch (e: Exception) {
                val cached = dbDataSource.get${prefix}All()
                if (cached.isNotEmpty()) {
                    Result.Success($pageType(list = cached.map { it.toDomain() }, total = cached.size.toLong()))
                } else {
                    Result.Failure(e)
                }
            }
        """.trimIndent()
    }

}
