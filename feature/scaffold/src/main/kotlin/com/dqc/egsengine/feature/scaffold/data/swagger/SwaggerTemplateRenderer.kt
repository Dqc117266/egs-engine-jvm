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
        val domainSimpleName = ctx.domainModelName(schema.name)
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
            "android/swagger/DataModel.kt.ftl",
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
        val imports = buildSet {
            schema.properties.forEach { prop ->
                addAll(ctx.importsForType(prop.type, forDomain = true, currentPackage = ctx.domainModelPackage))
            }
        }.sorted()
        return engine.render(
            "android/swagger/DomainModel.kt.ftl",
            mapOf(
                "packageName" to ctx.domainModelPackage,
                "className" to className,
                "props" to props,
                "imports" to imports,
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
        }.sorted()
        return engine.render(
            "android/swagger/ApiService.kt.ftl",
            mapOf(
                "packageName" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    fun renderRepositoryInterface(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
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
                "returnType" to ctx.repositoryReturnType(op.responseBody),
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
                addAll(ctx.importsForRepositoryReturnType(op.responseBody))
            }
        }.sorted()
        return engine.render(
            "android/swagger/ApiRepository.kt.ftl",
            mapOf(
                "packageName" to ctx.domainRepositoryPackage,
                "apiRepositoryName" to ctx.apiRepositoryName,
                "operations" to operations,
                "imports" to imports,
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
                val type = ctx.resolveType(param.type, forDomain = true)
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
        val imports = androidGeneratedRepositorySupportImports(spec, ctx)
        val repositoryInterface =
            if (ctx.usesSplitRepositoryLayout) ctx.apiRepositoryName else ctx.repositoryName
        return engine.render(
            "android/swagger/RepositoryImpl.kt.ftl",
            mapOf(
                "packageName" to ctx.dataRepositoryPackage,
                "repositoryImplName" to ctx.repositoryImplName,
                "repositoryName" to repositoryInterface,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    /**
     * Generated `open class GeneratedRepositorySupport` under `/generate/` (not overwritten by module scaffold).
     */
    fun renderGeneratedRepositorySupport(spec: SwaggerSpec, ctx: SwaggerGeneratorContext, projectRoot: File? = null): String {
        val operations = spec.operations.map { op ->
            val callArgs = mutableListOf<String>()
            val params = op.params.map { param ->
                val name = param.name.toSafeIdentifier()
                callArgs.add(name)
                val type = ctx.resolveType(param.type, forDomain = true)
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
        val imports = androidGeneratedRepositorySupportImports(spec, ctx)
        return engine.render(
            "android/swagger/GeneratedRepositorySupport.kt.ftl",
            mapOf(
                "packageName" to ctx.dataRepositoryPackage,
                "repositoryImplName" to ctx.repositoryImplName,
                "apiRepositoryName" to ctx.apiRepositoryName,
                "serviceName" to ctx.serviceName,
                "operations" to operations,
                "imports" to imports,
            ),
            projectRoot,
        )
    }

    /**
     * Import list for `GeneratedRepositorySupport` / `RepositoryImpl` (mirrors KMP [KmpSwaggerTemplateRenderer.renderApiRepositorySupport]).
     */
    private fun androidGeneratedRepositorySupportImports(
        spec: SwaggerSpec,
        ctx: SwaggerGeneratorContext,
    ): List<String> {
        val needsToDomainImport = spec.operations.any { ctx.requiresToDomainImport(it.responseBody) }
        val needsToDataImport = spec.operations.any { it.requestBody != null }
        val needsToResult = ctx.hasResultWrappers()
        val toResultPackage = ctx.template.toResultPackage ?: ""
        return buildSet {
            spec.operations.forEach { op ->
                op.params.forEach { param ->
                    addAll(ctx.importsForType(param.type, forDomain = true, currentPackage = ctx.dataRepositoryPackage))
                }
                op.requestBody?.let { body ->
                    addAll(ctx.importsForType(body, forDomain = true, currentPackage = ctx.dataRepositoryPackage))
                }
                addAll(ctx.importsForRepositoryReturnType(op.responseBody))
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
        }.sorted()
    }

    fun renderGeneratedDataModule(ctx: SwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "android/swagger/GeneratedDataModule.kt.ftl",
            mapOf(
                "generateDiPackage" to ctx.generateDiPackage,
                "servicePackage" to ctx.servicePackage,
                "serviceName" to ctx.serviceName,
                "dataRepositoryPackage" to ctx.dataRepositoryPackage,
                "apiRepositorySupportName" to ctx.apiRepositorySupportName,
            ),
            projectRoot,
        )

    fun renderGeneratedDomainModule(
        spec: SwaggerSpec,
        ctx: SwaggerGeneratorContext,
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
        val dbUseCases = preservedDbUseCaseClassNames.map { mapOf("useCaseClass" to it) }
        val dbUseCaseImports = preservedDbUseCaseClassNames.map { "${ctx.domainUseCasePackage}.$it" }
        val prefsUseCases = preservedPrefsUseCaseClassNames.map { mapOf("useCaseClass" to it) }
        val prefsUseCaseImports = preservedPrefsUseCaseClassNames.map { "${ctx.domainUseCasePackage}.$it" }
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

    fun renderDataModule(ctx: SwaggerGeneratorContext, projectRoot: File? = null): String =
        engine.render(
            "android/swagger/ApiDataModule.kt.ftl",
            mapOf(
                "dataPackage" to ctx.dataPackage,
                "dataRepositoryPackage" to ctx.dataRepositoryPackage,
                "repositoryImplName" to ctx.repositoryImplName,
                "domainRepositoryPackage" to ctx.domainRepositoryPackage,
                "apiRepositoryName" to ctx.apiRepositoryName,
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
            addAll(ctx.importsForRepositoryReturnType(op.responseBody))
            add("${ctx.domainRepositoryPackage}.${ctx.repositoryName}")
        }.sorted()
        return engine.render(
            "android/swagger/UseCase.kt.ftl",
            mapOf(
                "packageName" to ctx.domainUseCasePackage,
                "useCaseName" to useCaseName,
                "repositoryName" to ctx.repositoryName,
                "returnType" to ctx.repositoryReturnType(op.responseBody),
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
