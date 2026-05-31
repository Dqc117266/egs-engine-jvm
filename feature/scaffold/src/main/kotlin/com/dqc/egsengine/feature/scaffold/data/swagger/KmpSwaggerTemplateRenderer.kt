/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.template.TemplateEngine
import java.io.File

/**
 * Renders KMP Swagger-driven Kotlin sources using FreeMarker templates under `templates/kmp/swagger/`.
 */
class KmpSwaggerTemplateRenderer(
    private val engine: TemplateEngine,
) {

    fun renderDataModel(
        schema: SwaggerSchema,
        ctx: KmpSwaggerGeneratorContext,
        isRequestSchema: Boolean,
        projectRoot: File? = null,
    ): String {
        val className = ctx.dataModelName(schema.name)
        val domainSimpleName = ctx.domainModelName(schema.name)
        val domainClassName = "${ctx.domainModelPackage}.$domainSimpleName"
        val props = schema.properties.map { prop ->
            val kotlinType = ctx.resolveType(prop.type, forDomain = false)
            mapOf(
                "name" to prop.name,
                "originalName" to prop.originalName,
                "kotlinType" to kotlinType,
                "nullable" to (!prop.required),
                "toDomainExpr" to ctx.toDomainExpression(prop.type, "this.${prop.name}", !prop.required),
                "toDataExpr" to ctx.toDataExpression(prop.type, "this.${prop.name}", !prop.required),
            )
        }
        val imports = buildSet {
            schema.properties.forEach { prop ->
                addAll(ctx.importsForType(prop.type, forDomain = false, currentPackage = ctx.dataModelPackage))
            }
            add("${ctx.domainModelPackage}.$domainSimpleName")
        }.sorted()
        return engine.render(
            "kmp/swagger/KmpDataModel.kt.ftl",
            mapOf(
                "packageName" to ctx.dataModelPackage,
                "className" to className,
                "domainSimpleName" to domainSimpleName,
                "props" to props,
                "hasToData" to isRequestSchema,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    fun renderDomainModel(schema: SwaggerSchema, ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String {
        val className = ctx.domainModelName(schema.name)
        val props = schema.properties.map { prop ->
            val kotlinType = ctx.resolveType(prop.type, forDomain = true)
            mapOf(
                "name" to prop.name,
                "kotlinType" to kotlinType,
                "nullable" to (!prop.required),
            )
        }
        val imports = buildSet {
            schema.properties.forEach { prop ->
                addAll(ctx.importsForType(prop.type, forDomain = true, currentPackage = ctx.domainModelPackage))
            }
        }.sorted()
        return engine.render(
            "kmp/swagger/DomainModel.kt.ftl",
            mapOf(
                "packageName" to ctx.domainModelPackage,
                "className" to className,
                "props" to props,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    fun renderKtorfitServiceInterface(
        spec: SwaggerSpec,
        ctx: KmpSwaggerGeneratorContext,
        projectRoot: File? = null,
    ): String {
        val operations = spec.operations.map { op ->
            val params = op.params.map { param ->
                val type = ctx.resolveType(param.type, forDomain = false)
                    .let { if (!param.required) "$it?" else it }
                val loc = param.location.lowercase()
                mapOf(
                    "name" to param.name.toSafeIdentifier(),
                    "type" to type,
                    "originalName" to param.originalName,
                    "location" to loc,
                    "pathAnnotation" to (loc == "path"),
                )
            }
            val requestBodyType = op.requestBody?.let { ctx.resolveType(it, forDomain = false) }
            mapOf(
                "operationId" to op.operationId,
                "method" to op.method,
                "path" to op.path,
                "returnType" to ctx.serviceReturnType(op.responseBody),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to requestBodyType,
                "methodAnnotationImport" to ctx.ktorfitMethodAnnotationImport(op.method),
                "methodAnnotationSimple" to ctx.ktorfitMethodAnnotationSimple(op.method),
            )
        }
        val typeImports = buildSet {
            spec.operations.forEach { op ->
                op.params.forEach { param ->
                    addAll(ctx.importsForType(param.type, forDomain = false, currentPackage = ctx.servicePackage))
                }
                op.requestBody?.let { body ->
                    addAll(ctx.importsForType(body, forDomain = false, currentPackage = ctx.servicePackage))
                }
                addAll(ctx.importsForServiceReturnType(op.responseBody))
            }
        }
        val imports = buildSet {
            addAll(typeImports)
            if (operations.any { it["hasBody"] as Boolean }) {
                add("de.jensklingenberg.ktorfit.http.Body")
            }
            operations.forEach { op ->
                add(op["methodAnnotationImport"] as String)
                (op["params"] as List<*>).forEach { p ->
                    val m = p as Map<*, *>
                    add(
                        if (m["pathAnnotation"] as Boolean) {
                            "de.jensklingenberg.ktorfit.http.Path"
                        } else {
                            "de.jensklingenberg.ktorfit.http.Query"
                        },
                    )
                }
            }
        }.sorted()
        return engine.render(
            "kmp/swagger/KtorfitApiService.kt.ftl",
            mapOf(
                "packageName" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    fun renderRepositoryInterface(spec: SwaggerSpec, ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String {
        val operations = spec.operations.map { op ->
            val params = op.params.map { param ->
                val type = ctx.resolveType(param.type, forDomain = true)
                    .let { if (!param.required) "$it?" else it }
                mapOf(
                    "name" to param.name.toSafeIdentifier(),
                    "type" to type,
                )
            }
            val bodyType = op.requestBody?.let { ctx.resolveType(it, forDomain = true) }
            mapOf(
                "operationId" to op.operationId,
                "returnType" to ctx.repositoryReturnType(op),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to bodyType,
            )
        }
        val imports = buildSet {
            spec.operations.forEach { op ->
                op.params.forEach { param ->
                    addAll(ctx.importsForType(param.type, forDomain = true, currentPackage = ctx.domainRepositoryPackage))
                }
                op.requestBody?.let { body ->
                    addAll(ctx.importsForType(body, forDomain = true, currentPackage = ctx.domainRepositoryPackage))
                }
                addAll(ctx.importsForRepositoryReturnType(op))
            }
        }.sorted()
        return engine.render(
            "kmp/swagger/ApiRepository.kt.ftl",
            mapOf(
                "packageName" to ctx.domainRepositoryPackage,
                "apiRepositoryName" to ctx.apiRepositoryName,
                "operations" to operations,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    fun defaultRepositorySupportStatement(op: SwaggerOperation, ctx: KmpSwaggerGeneratorContext): String {
        val callArgs = mutableListOf<String>()
        op.params.forEach { param ->
            callArgs.add(param.name.toSafeIdentifier())
        }
        if (op.requestBody != null) {
            callArgs.add(
                if (op.requestBody is SwaggerType.ModelRef) "body.toData()" else "body"
            )
        }
        val serviceCall = "service.${op.operationId}(${callArgs.joinToString(", ")})"
        val mapperExpr = if (op.paging != null) {
            ctx.pagingRepositoryMapExpression(op, "it")
        } else {
            ctx.repositoryResponseMapExpression(op.responseBody, "it")
        }
        return if (ctx.hasResultWrappers()) {
            if (mapperExpr != null) {
                "return $serviceCall.toResult { $mapperExpr }"
            } else {
                "return $serviceCall.toResult()"
            }
        } else {
            if (mapperExpr != null) {
                val mapped = if (op.paging != null) {
                    ctx.pagingRepositoryMapExpression(op, serviceCall)
                } else {
                    ctx.repositoryResponseMapExpression(op.responseBody, serviceCall)
                }
                "return $mapped"
            } else {
                "return $serviceCall"
            }
        }
    }

    fun renderApiRepositorySupport(
        spec: SwaggerSpec,
        ctx: KmpSwaggerGeneratorContext,
        projectRoot: File? = null,
        statementOverride: (SwaggerOperation, String) -> String = { _, stmt -> stmt },
        extraImports: Set<String> = emptySet(),
    ): String {
        val operations = spec.operations.map { op ->
            val params = op.params.map { param ->
                val name = param.name.toSafeIdentifier()
                val type = ctx.resolveType(param.type, forDomain = true)
                    .let { if (!param.required) "$it?" else it }
                mapOf("name" to name, "type" to type)
            }
            val stmt = defaultRepositorySupportStatement(op, ctx)
            mapOf(
                "operationId" to op.operationId,
                "returnType" to ctx.repositoryReturnType(op),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to op.requestBody?.let { ctx.resolveType(it, forDomain = true) },
                "statement" to statementOverride(op, stmt),
            )
        }
        val needsToDomainImport = spec.operations.any { ctx.requiresToDomainImportForOperation(it) }
        val needsToDataImport = spec.operations.any { it.requestBody is SwaggerType.ModelRef }
        val needsToResult = ctx.hasResultWrappers()
        val toResultPackage = ctx.template.toResultPackage ?: ""
        val imports = buildSet {
            spec.operations.forEach { op ->
                op.params.forEach { param ->
                    addAll(ctx.importsForType(param.type, forDomain = true, currentPackage = ctx.dataRepositoryPackage))
                }
                op.requestBody?.let { body ->
                    addAll(ctx.importsForType(body, forDomain = true, currentPackage = ctx.dataRepositoryPackage))
                }
                addAll(ctx.importsForRepositoryReturnType(op))
            }
            add("${ctx.servicePackage}.${ctx.serviceName}")
            add("${ctx.domainRepositoryPackage}.${ctx.apiRepositoryName}")
            if (needsToResult && ctx.template.baseClassPackages.resultClass != null) {
                add(ctx.template.baseClassPackages.resultClass!!)
            }
            if (needsToResult && toResultPackage.isNotBlank()) {
                add("$toResultPackage.toResult")
            }
            if (needsToDomainImport) {
                add("${ctx.dataModelPackage}.toDomain")
            }
            if (needsToDataImport) {
                add("${ctx.dataModelPackage}.toData")
            }
            addAll(extraImports)
        }.sorted()
        return engine.render(
            "kmp/swagger/ApiRepositorySupport.kt.ftl",
            mapOf(
                "packageName" to ctx.dataRepositoryPackage,
                "apiRepositorySupportName" to ctx.apiRepositorySupportName,
                "apiRepositoryName" to ctx.apiRepositoryName,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    fun renderGeneratedDataModule(ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "kmp/swagger/GeneratedDataModule.kt.ftl",
            mapOf(
                "generateDiPackage" to ctx.generateDiPackage,
                "pascalModuleName" to ctx.pascalModuleName,
                "servicePackage" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "dataRepositoryPackage" to ctx.dataRepositoryPackage,
                "apiRepositorySupportName" to ctx.apiRepositorySupportName,
            ),
            projectRoot,
        )

    fun renderGeneratedDomainModule(
        spec: SwaggerSpec,
        ctx: KmpSwaggerGeneratorContext,
        preservedDbUseCaseClassNames: List<String> = emptyList(),
        preservedPrefsUseCaseClassNames: List<String> = emptyList(),
        projectRoot: File? = null,
    ): String {
        val swaggerUseCases = spec.operations.map { op ->
            mapOf(
                "useCaseClass" to "${op.operationId.toSafePascal()}UseCase",
                "domainUseCasePackage" to ctx.domainUseCasePackage,
            )
        }
        val dbUseCases = preservedDbUseCaseClassNames.map { simpleName ->
            mapOf("useCaseClass" to simpleName)
        }
        val dbUseCaseImports = preservedDbUseCaseClassNames.map { simpleName ->
            "${ctx.domainUseCasePackage}.$simpleName"
        }
        val prefsUseCases = preservedPrefsUseCaseClassNames.map { simpleName ->
            mapOf("useCaseClass" to simpleName)
        }
        val prefsUseCaseImports = preservedPrefsUseCaseClassNames.map { simpleName ->
            "${ctx.domainUseCasePackage}.$simpleName"
        }
        return engine.render(
            "kmp/swagger/GeneratedDomainModule.kt.ftl",
            mapOf(
                "generateDiPackage" to ctx.generateDiPackage,
                "swaggerUseCases" to swaggerUseCases,
                "dbUseCases" to dbUseCases,
                "dbUseCaseImports" to dbUseCaseImports,
                "prefsUseCases" to prefsUseCases,
                "prefsUseCaseImports" to prefsUseCaseImports,
            ),
            projectRoot,
        )
    }

    fun renderUseCase(op: SwaggerOperation, ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String {
        val useCaseName = "${op.operationId.toSafePascal()}UseCase"
        val args = mutableListOf<String>()
        val params = op.params.map { param ->
            val name = param.name.toSafeIdentifier()
            args.add(name)
            val type = ctx.resolveType(param.type, forDomain = true)
                .let { if (!param.required) "$it?" else it }
            mapOf("name" to name, "type" to type)
        }
        if (op.requestBody != null) {
            args.add("body")
        }
        val bodyType = op.requestBody?.let { ctx.resolveType(it, forDomain = true) }
        val imports = buildSet {
            op.params.forEach { param ->
                addAll(ctx.importsForType(param.type, forDomain = true, currentPackage = ctx.domainUseCasePackage))
            }
            op.requestBody?.let { body ->
                addAll(ctx.importsForType(body, forDomain = true, currentPackage = ctx.domainUseCasePackage))
            }
            addAll(ctx.importsForRepositoryReturnType(op))
            add("${ctx.domainRepositoryPackage}.${ctx.combinedRepositoryName}")
        }.sorted()
        return engine.render(
            "kmp/swagger/UseCase.kt.ftl",
            mapOf(
                "packageName" to ctx.domainUseCasePackage,
                "useCaseName" to useCaseName,
                "repositoryName" to ctx.combinedRepositoryName,
                "returnType" to ctx.repositoryReturnType(op),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to bodyType,
                "operationId" to op.operationId,
                "callArgs" to args.joinToString(", "),
                "imports" to imports,
            ),
            projectRoot,
        )
    }
}
