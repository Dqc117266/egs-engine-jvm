/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.template.TemplateEngine
import java.io.File

/**
 * Renders Swagger-driven Kotlin sources using FreeMarker templates under `templates/android/swagger/`.
 */
class SwaggerTemplateRenderer(
    private val engine: TemplateEngine,
) {

    fun renderDataModel(
        schema: SwaggerSchema,
        ctx: SwaggerGeneratorContext,
        isRequestSchema: Boolean,
        projectRoot: File? = null,
    ): String {
        val className = ctx.dataModelName(schema.name)
        val domainClassName = "${ctx.domainModelPackage}.${ctx.domainModelName(schema.name)}"
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
            "android/swagger/DataModel.kt.ftl",
            mapOf(
                "packageName" to ctx.dataModelPackage,
                "className" to className,
                "domainClassName" to domainClassName,
                "props" to props,
                "hasToData" to isRequestSchema,
            ),
            projectRoot,
        )
    }

    fun renderDomainModel(schema: SwaggerSchema, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
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
            "android/swagger/DomainModel.kt.ftl",
            mapOf(
                "packageName" to ctx.domainModelPackage,
                "className" to className,
                "props" to props,
            ),
            projectRoot,
        )
    }

    fun renderServiceInterface(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
        val operations = spec.operations.map { op ->
            val params = op.params.map { param ->
                val type = ctx.resolveType(param.type, forDomain = false)
                    .let { if (!param.required) "$it?" else it }
                mapOf(
                    "name" to param.name.toSafeIdentifier(),
                    "type" to type,
                    "originalName" to param.originalName,
                    "location" to param.location.lowercase(),
                    "pathAnnotation" to (param.location.lowercase() == "path"),
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
                "methodAnnotationImport" to ctx.retrofitMethodAnnotationImport(op.method),
                "methodAnnotationSimple" to ctx.retrofitMethodAnnotationSimple(op.method),
            )
        }
        val imports = buildSet {
            if (operations.any { it["hasBody"] as Boolean }) {
                add("retrofit2.http.Body")
            }
            operations.forEach { op ->
                add(op["methodAnnotationImport"] as String)
                (op["params"] as List<*>).forEach { p ->
                    val m = p as Map<*, *>
                    add(
                        if (m["pathAnnotation"] as Boolean) "retrofit2.http.Path" else "retrofit2.http.Query",
                    )
                }
            }
        }
        return engine.render(
            "android/swagger/ApiService.kt.ftl",
            mapOf(
                "packageName" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports.sorted(),
            ),
            projectRoot,
        )
    }

    fun renderRepositoryInterface(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
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
            "android/swagger/Repository.kt.ftl",
            mapOf(
                "packageName" to ctx.domainRepositoryPackage,
                "repositoryName" to ctx.repositoryName,
                "operations" to operations,
            ),
            projectRoot,
        )
    }

    fun renderRepositoryImpl(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
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
        return engine.render(
            "android/swagger/RepositoryImpl.kt.ftl",
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
                "operations" to operations,
            ),
            projectRoot,
        )
    }

    /**
     * Generated `open class Generated¡­RepositorySupport` under `¡­/generate/¡­` (not overwritten by module scaffold).
     */
    fun renderGeneratedRepositorySupport(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
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
        return engine.render(
            "android/swagger/GeneratedRepositorySupport.kt.ftl",
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
                "operations" to operations,
            ),
            projectRoot,
        )
    }

    fun renderGeneratedDataModule(ctx: SwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "android/swagger/GeneratedDataModule.kt.ftl",
            mapOf(
                "generateDiPackage" to ctx.generateDiPackage,
                "servicePackage" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
            ),
            projectRoot,
        )

    fun renderGeneratedDomainModule(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
        val useCases = spec.operations.map { op ->
            mapOf(
                "useCaseClass" to "${op.operationId.toSafePascal()}UseCase",
                "domainUseCasePackage" to ctx.domainUseCasePackage,
            )
        }
        return engine.render(
            "android/swagger/GeneratedDomainModule.kt.ftl",
            mapOf(
                "generateDiPackage" to ctx.generateDiPackage,
                "useCases" to useCases,
            ),
            projectRoot,
        )
    }

    fun renderDataModule(ctx: SwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "android/swagger/ApiDataModule.kt.ftl",
            mapOf(
                "dataPackage" to ctx.dataPackage,
                "dataRepositoryPackage" to ctx.dataRepositoryPackage,
                "repositoryImplName" to ctx.repositoryImplName,
                "domainRepositoryPackage" to ctx.domainRepositoryPackage,
                "repositoryName" to ctx.repositoryName,
                "servicePackage" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
            ),
            projectRoot,
        )

    fun renderDomainModule(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
        val useCases = spec.operations.map { op ->
            mapOf(
                "useCaseClass" to "${op.operationId.toSafePascal()}UseCase",
                "domainUseCasePackage" to ctx.domainUseCasePackage,
            )
        }
        return engine.render(
            "android/swagger/ApiDomainModule.kt.ftl",
            mapOf(
                "domainPackage" to ctx.domainPackage,
                "useCases" to useCases,
            ),
            projectRoot,
        )
    }

    fun renderRootKoinModule(ctx: SwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "android/swagger/ApiRootKoinModule.kt.ftl",
            mapOf(
                "rootPackage" to ctx.rootPackage,
                "pascalModuleName" to ctx.pascalModuleName,
                "domainPackage" to ctx.domainPackage,
                "dataPackage" to ctx.dataPackage,
            ),
            projectRoot,
        )

    fun renderUseCase(op: SwaggerOperation, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
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
            "android/swagger/UseCase.kt.ftl",
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
