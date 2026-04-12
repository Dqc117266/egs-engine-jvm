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
        return engine.render(
            "kmp/swagger/KmpDataModel.kt.ftl",
            mapOf(
                "packageName" to ctx.dataModelPackage,
                "className" to className,
                "domainSimpleName" to domainSimpleName,
                "domainClassName" to domainClassName,
                "props" to props,
                "hasToData" to isRequestSchema,
                "domainModelPackage" to ctx.domainModelPackage,
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
        return engine.render(
            "kmp/swagger/DomainModel.kt.ftl",
            mapOf(
                "packageName" to ctx.domainModelPackage,
                "className" to className,
                "props" to props,
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
        val imports = buildSet {
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
        }
        return engine.render(
            "kmp/swagger/KtorfitApiService.kt.ftl",
            mapOf(
                "packageName" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports.sorted(),
            ),
            projectRoot,
        )
    }

    fun renderRepositoryInterface(spec: SwaggerSpec, ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String {
        val operations = spec.operations.map { op ->
            val params = op.params.map { param ->
                val type = ctx.resolveType(param.type, forDomain = false)
                    .let { if (!param.required) "$it?" else it }
                mapOf(
                    "name" to param.name.toSafeIdentifier(),
                    "type" to type,
                )
            }
            val bodyType = op.requestBody?.let { ctx.resolveType(it, forDomain = true) }
            mapOf(
                "operationId" to op.operationId,
                "returnType" to ctx.repositoryReturnType(op.responseBody),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to bodyType,
            )
        }
        return engine.render(
            "kmp/swagger/Repository.kt.ftl",
            mapOf(
                "packageName" to ctx.domainRepositoryPackage,
                "repositoryName" to ctx.repositoryName,
                "operations" to operations,
            ),
            projectRoot,
        )
    }

    fun renderGeneratedRepositorySupport(spec: SwaggerSpec, ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String {
        val operations = spec.operations.map { op ->
            val callArgs = mutableListOf<String>()
            val params = op.params.map { param ->
                val name = param.name.toSafeIdentifier()
                callArgs.add(name)
                val type = ctx.resolveType(param.type, forDomain = false)
                    .let { if (!param.required) "$it?" else it }
                mapOf("name" to name, "type" to type)
            }
            if (op.requestBody != null) {
                callArgs.add("body.toData()")
            }
            val serviceCall = "service.${op.operationId}(${callArgs.joinToString(", ")})"
            val mapperExpr = ctx.repositoryResponseMapExpression(op.responseBody, "it")
            val stmt = if (ctx.hasResultWrappers()) {
                if (mapperExpr != null) {
                    "return $serviceCall.toResult { $mapperExpr }"
                } else {
                    "return $serviceCall.toResult()"
                }
            } else {
                if (mapperExpr != null) {
                    val mapped = ctx.repositoryResponseMapExpression(op.responseBody, serviceCall)
                    "return $mapped"
                } else {
                    "return $serviceCall"
                }
            }
            mapOf(
                "operationId" to op.operationId,
                "returnType" to ctx.repositoryReturnType(op.responseBody),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to op.requestBody?.let { ctx.resolveType(it, forDomain = true) },
                "statement" to stmt,
            )
        }
        val needsToDomainImport = spec.operations.any { ctx.requiresToDomainImport(it.responseBody) }
        val needsToDataImport = spec.operations.any { it.requestBody != null }
        val needsToResult = ctx.hasResultWrappers()
        val toResultPackage = ctx.template.toResultPackage ?: ""
        val resultClassFqn = ctx.template.baseClassPackages.resultClass ?: ""
        return engine.render(
            "kmp/swagger/GeneratedRepositorySupport.kt.ftl",
            mapOf(
                "packageName" to ctx.dataRepositoryPackage,
                "repositoryImplName" to ctx.repositoryImplName,
                "repositoryName" to ctx.repositoryName,
                "servicePackage" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "domainRepositoryPackage" to ctx.domainRepositoryPackage,
                "dataModelPackage" to ctx.dataModelPackage,
                "needsToDomainImport" to needsToDomainImport,
                "needsToDataImport" to needsToDataImport,
                "needsToResult" to needsToResult,
                "toResultPackage" to toResultPackage,
                "resultClassFqn" to resultClassFqn,
                "operations" to operations,
            ),
            projectRoot,
        )
    }

    fun renderGeneratedDataModule(ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "kmp/swagger/GeneratedDataModule.kt.ftl",
            mapOf(
                "generateRootPackage" to ctx.generateRootPackage,
                "pascalModuleName" to ctx.pascalModuleName,
                "servicePackage" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
            ),
            projectRoot,
        )

    fun renderGeneratedDomainModule(spec: SwaggerSpec, ctx: KmpSwaggerGeneratorContext, projectRoot: File? = null): String {
        val useCases = spec.operations.map { op ->
            mapOf(
                "useCaseClass" to "${op.operationId.toSafePascal()}UseCase",
                "domainUseCasePackage" to ctx.domainUseCasePackage,
            )
        }
        return engine.render(
            "kmp/swagger/GeneratedDomainModule.kt.ftl",
            mapOf(
                "generateRootPackage" to ctx.generateRootPackage,
                "useCases" to useCases,
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
            val type = ctx.resolveType(param.type, forDomain = false)
                .let { if (!param.required) "$it?" else it }
            mapOf("name" to name, "type" to type)
        }
        if (op.requestBody != null) {
            args.add("body")
        }
        val bodyType = op.requestBody?.let { ctx.resolveType(it, forDomain = true) }
        return engine.render(
            "kmp/swagger/UseCase.kt.ftl",
            mapOf(
                "packageName" to ctx.domainUseCasePackage,
                "useCaseName" to useCaseName,
                "repositoryPackage" to ctx.domainRepositoryPackage,
                "repositoryName" to ctx.repositoryName,
                "returnType" to ctx.repositoryReturnType(op.responseBody),
                "params" to params,
                "hasBody" to (op.requestBody != null),
                "bodyType" to bodyType,
                "operationId" to op.operationId,
                "callArgs" to args.joinToString(", "),
            ),
            projectRoot,
        )
    }
}
