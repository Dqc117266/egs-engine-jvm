package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import java.io.File

class FtlSwaggerCodeGenerator(
    private val templateEngine: TemplateEngine = TemplateEngine(TemplateRegistry()),
) {

    fun generate(projectRoot: File, template: ModuleTemplate, spec: SwaggerSpec): List<ModuleGenerator.GeneratedFile> {
        val ctx = SwaggerTemplateContext(template)
        val (wrapperSchemas, dataSchemas) = spec.schemas.partition { it.isCommonResultWrapper() }
        val wrapperUnwrapMap = wrapperSchemas.associate { schema ->
            schema.name to schema.properties.firstOrNull { it.originalName == "data" }?.type
        }
        val adjustedSpec = spec.copy(
            operations = spec.operations.map { operation ->
                operation.copy(
                    params = operation.params.filter { it.location.lowercase() != "header" },
                    responseBody = operation.responseBody.unwrap(wrapperUnwrapMap),
                )
            },
        )
        val requestSchemaNames = spec.collectRequestSchemaNames()
        val files = mutableListOf<ModuleGenerator.GeneratedFile>()

        dataSchemas.forEach { schema ->
            files += renderDataModel(projectRoot, ctx, schema, schema.name in requestSchemaNames)
            files += renderDomainModel(projectRoot, ctx, schema)
        }

        files += renderApiService(projectRoot, ctx, adjustedSpec)
        files += renderApiRepository(projectRoot, ctx, adjustedSpec)
        files += renderRepositoryImpl(projectRoot, ctx, adjustedSpec)
        files += renderDataModule(projectRoot, ctx)
        files += renderDomainModule(projectRoot, ctx, adjustedSpec)
        if (!ctx.kmp) {
            files += renderRootKoinModule(projectRoot, ctx)
        }
        adjustedSpec.operations.forEach { operation ->
            files += renderUseCase(projectRoot, ctx, operation)
        }

        return files
    }

    private fun renderDataModel(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        schema: SwaggerSchema,
        hasToData: Boolean,
    ): ModuleGenerator.GeneratedFile {
        val className = ctx.dataModelName(schema.name)
        val domainSimpleName = ctx.domainModelName(schema.name)
        val props = schema.properties.map { property ->
            val dataType = ctx.typeRef(property.type, forDomain = false)
            mapOf(
                "name" to property.name.toSafeIdentifier(),
                "originalName" to property.originalName,
                "kotlinType" to dataType.code,
                "nullable" to !property.required,
                "toDomainExpr" to ctx.toDomainExpression(property.type, "this.${property.name.toSafeIdentifier()}", !property.required),
                "toDataExpr" to ctx.toDataExpression(property.type, "this.${property.name.toSafeIdentifier()}", !property.required),
            )
        }
        val imports = buildSet {
            add("${ctx.domainModelPackage}.$domainSimpleName")
            schema.properties.forEach { property ->
                addAll(ctx.typeRef(property.type, forDomain = false).imports)
            }
        }.filterImports(ctx.dataModelPackage)
        val model = mapOf(
            "packageName" to ctx.dataModelPackage,
            "className" to className,
            "domainSimpleName" to domainSimpleName,
            "props" to props,
            "imports" to imports,
            "hasToData" to hasToData,
        )
        return ctx.render(projectRoot, ctx.dataModelTemplate, model, "${ctx.dataModelPath}/$className.kt")
    }

    private fun renderDomainModel(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        schema: SwaggerSchema,
    ): ModuleGenerator.GeneratedFile {
        val className = ctx.domainModelName(schema.name)
        val props = schema.properties.map { property ->
            val domainType = ctx.typeRef(property.type, forDomain = true)
            mapOf(
                "name" to property.name.toSafeIdentifier(),
                "kotlinType" to domainType.code,
                "nullable" to !property.required,
            )
        }
        val imports = schema.properties
            .flatMap { ctx.typeRef(it.type, forDomain = true).imports }
            .toSet()
            .filterImports(ctx.domainModelPackage)
        val model = mapOf(
            "packageName" to ctx.domainModelPackage,
            "className" to className,
            "props" to props,
            "imports" to imports,
        )
        return ctx.render(projectRoot, "${ctx.templateRoot}/DomainModel.kt.ftl", model, "${ctx.domainModelPath}/$className.kt")
    }

    private fun renderApiService(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        spec: SwaggerSpec,
    ): ModuleGenerator.GeneratedFile {
        val operations = spec.operations.map { ctx.operationModel(it, serviceTypes = true) }
        val imports = buildSet {
            spec.operations.forEach { operation ->
                val httpPackage = if (ctx.kmp) "de.jensklingenberg.ktorfit.http" else "retrofit2.http"
                add("$httpPackage.${operation.method.toRetrofitMethod()}")
                if (operation.params.any { it.location.lowercase() == "path" }) add("$httpPackage.Path")
                if (operation.params.any { it.location.lowercase() != "path" }) add("$httpPackage.Query")
                if (operation.requestBody != null) add("$httpPackage.Body")
                operation.requestBody?.let { addAll(ctx.typeRef(it, forDomain = false).imports) }
                addAll(ctx.serviceReturnType(operation.responseBody).imports)
            }
        }.filterImports(ctx.servicePackage)
        val model = mapOf(
            "packageName" to ctx.servicePackage,
            "serviceName" to ctx.serviceName,
            "operations" to operations,
            "imports" to imports,
        )
        return ctx.render(projectRoot, ctx.serviceTemplate, model, "${ctx.servicePath}/${ctx.serviceName}.kt")
    }

    private fun renderApiRepository(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        spec: SwaggerSpec,
    ): ModuleGenerator.GeneratedFile {
        val operations = spec.operations.map { ctx.operationModel(it, serviceTypes = false) }
        val imports = buildSet {
            spec.operations.forEach { operation ->
                operation.requestBody?.let { addAll(ctx.typeRef(it, forDomain = true).imports) }
                addAll(ctx.repositoryReturnType(operation.responseBody).imports)
            }
        }.filterImports(ctx.domainRepositoryPackage)
        val model = ctx.repositoryModel(operations, imports)
        return ctx.render(projectRoot, "${ctx.templateRoot}/ApiRepository.kt.ftl", model, "${ctx.domainRepositoryPath}/${ctx.repositoryName}.kt")
    }

    private fun renderRepositoryImpl(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        spec: SwaggerSpec,
    ): ModuleGenerator.GeneratedFile {
        val operations = spec.operations.map { operation ->
            ctx.operationModel(operation, serviceTypes = false) + ("statement" to ctx.repositoryStatement(operation))
        }
        val imports = buildSet {
            add("${ctx.servicePackage}.${ctx.serviceName}")
            add("${ctx.domainRepositoryPackage}.${ctx.repositoryName}")
            spec.operations.forEach { operation ->
                operation.requestBody?.let {
                    addAll(ctx.typeRef(it, forDomain = true).imports)
                    add("${ctx.dataModelPackage}.toData")
                }
                addAll(ctx.repositoryReturnType(operation.responseBody).imports)
                if (ctx.requiresToDomainImport(operation.responseBody)) {
                    add("${ctx.dataModelPackage}.toDomain")
                }
            }
            if (ctx.hasResultWrappers()) {
                ctx.template.toResultPackage?.let { add("$it.toResult") }
            }
        }.filterImports(ctx.dataRepositoryPackage)
        val model = ctx.repositoryModel(operations, imports) +
            ("repositoryImplName" to ctx.repositoryImplName) +
            ("apiRepositorySupportName" to ctx.repositoryImplName) +
            ("serviceName" to ctx.serviceName)
        return ctx.render(projectRoot, ctx.repositoryImplTemplate, model, "${ctx.dataRepositoryPath}/${ctx.repositoryImplName}.kt")
    }

    private fun renderDataModule(projectRoot: File, ctx: SwaggerTemplateContext): ModuleGenerator.GeneratedFile {
        val model = ctx.moduleModel()
        return ctx.render(projectRoot, ctx.dataModuleTemplate, model, ctx.dataModulePath)
    }

    private fun renderDomainModule(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        spec: SwaggerSpec,
    ): ModuleGenerator.GeneratedFile {
        val useCases = spec.operations.map { ctx.useCaseModel(it) }
        val model = ctx.moduleModel() + mapOf(
            "useCases" to useCases,
            "swaggerUseCases" to useCases,
            "dbUseCases" to emptyList<Map<String, String>>(),
            "dbUseCaseImports" to emptyList<String>(),
            "prefsUseCases" to emptyList<Map<String, String>>(),
            "prefsUseCaseImports" to emptyList<String>(),
        )
        return ctx.render(projectRoot, ctx.domainModuleTemplate, model, ctx.domainModulePath)
    }

    private fun renderRootKoinModule(projectRoot: File, ctx: SwaggerTemplateContext): ModuleGenerator.GeneratedFile =
        ctx.render(projectRoot, "${ctx.templateRoot}/ApiRootKoinModule.kt.ftl", ctx.moduleModel(), "${ctx.rootPath}/${ctx.pascalModuleName}KoinModule.kt")

    private fun renderUseCase(
        projectRoot: File,
        ctx: SwaggerTemplateContext,
        operation: SwaggerOperation,
    ): ModuleGenerator.GeneratedFile {
        val model = ctx.useCaseModel(operation) + mapOf(
            "packageName" to ctx.domainUseCasePackage,
            "repositoryName" to ctx.repositoryName,
            "operationId" to operation.operationId.toSafeIdentifier(),
        )
        return ctx.render(projectRoot, "${ctx.templateRoot}/UseCase.kt.ftl", model, "${ctx.domainUseCasePath}/${model["useCaseClass"]}.kt")
    }

    private fun SwaggerTemplateContext.render(
        projectRoot: File,
        templateName: String,
        model: Any,
        outputPath: String,
    ): ModuleGenerator.GeneratedFile =
        ModuleGenerator.GeneratedFile(
            path = outputPath,
            content = templateEngine.render(templateName, model, projectRoot),
        )
}

private class SwaggerTemplateContext(val template: ModuleTemplate) {
    val kmp: Boolean = template.projectType in setOf("KMP", "KMP_ANDROID")
    val templateRoot: String = if (kmp) "kmp/swagger" else "android/swagger"
    val moduleDir: String = "feature/${template.name}"
    val sourceSet: String = if (kmp) "commonMain" else "main"
    val sourceRoot: String = "$moduleDir/src/$sourceSet/kotlin"
    val pascalModuleName: String = template.name.toSafePascal()
    val rootPackage: String = template.packageName
    val rootPath: String = "$sourceRoot/${rootPackage.toPath()}"
    val dataPackage: String = "$rootPackage.data"
    val domainPackage: String = "$rootPackage.domain"
    val dataModelPackage: String = "$dataPackage.datasource.api.model"
    val servicePackage: String = "$dataPackage.datasource.api.service"
    val dataRepositoryPackage: String = "$dataPackage.repository"
    val domainModelPackage: String = "$domainPackage.model"
    val domainRepositoryPackage: String = "$domainPackage.repository"
    val domainUseCasePackage: String = "$domainPackage.usecase"
    val generateDiPackage: String = "$rootPackage.generate.di"
    val dataModelPath: String = "$sourceRoot/${dataModelPackage.toPath()}"
    val servicePath: String = "$sourceRoot/${servicePackage.toPath()}"
    val dataRepositoryPath: String = "$sourceRoot/${dataRepositoryPackage.toPath()}"
    val domainModelPath: String = "$sourceRoot/${domainModelPackage.toPath()}"
    val domainRepositoryPath: String = "$sourceRoot/${domainRepositoryPackage.toPath()}"
    val domainUseCasePath: String = "$sourceRoot/${domainUseCasePackage.toPath()}"
    val serviceName: String = if (kmp) "${pascalModuleName}ApiService" else "${pascalModuleName}RetrofitService"
    val repositoryName: String = "${pascalModuleName}Repository"
    val repositoryImplName: String = if (kmp) "${pascalModuleName}ApiRepositorySupport" else "${pascalModuleName}RepositoryImpl"
    val dataModelTemplate: String = if (kmp) "$templateRoot/KmpDataModel.kt.ftl" else "$templateRoot/DataModel.kt.ftl"
    val serviceTemplate: String = if (kmp) "$templateRoot/KtorfitApiService.kt.ftl" else "$templateRoot/ApiService.kt.ftl"
    val repositoryImplTemplate: String = if (kmp) "$templateRoot/ApiRepositorySupport.kt.ftl" else "$templateRoot/RepositoryImpl.kt.ftl"
    val dataModuleTemplate: String = if (kmp) "$templateRoot/GeneratedDataModule.kt.ftl" else "$templateRoot/ApiDataModule.kt.ftl"
    val domainModuleTemplate: String = if (kmp) "$templateRoot/GeneratedDomainModule.kt.ftl" else "$templateRoot/ApiDomainModule.kt.ftl"
    val dataModulePath: String = if (kmp) {
        "$sourceRoot/${generateDiPackage.toPath()}/GeneratedDataModule.kt"
    } else {
        "$sourceRoot/${dataPackage.toPath()}/dataModule.kt"
    }
    val domainModulePath: String = if (kmp) {
        "$sourceRoot/${generateDiPackage.toPath()}/GeneratedDomainModule.kt"
    } else {
        "$sourceRoot/${domainPackage.toPath()}/domainModule.kt"
    }

    fun dataModelName(rawName: String): String = "${rawName.toSafePascal()}ApiModel"
    fun domainModelName(rawName: String): String = rawName.toSafePascal()
    fun hasResultWrappers(): Boolean =
        template.apiResultClass != null && template.commonResultClass != null && template.toResultPackage != null

    fun typeRef(type: SwaggerType, forDomain: Boolean): CodeRef = when (type) {
        is SwaggerType.Primitive -> CodeRef(
            when (type.kind) {
                PrimitiveKind.STRING -> "String"
                PrimitiveKind.INT -> "Int"
                PrimitiveKind.LONG -> "Long"
                PrimitiveKind.DOUBLE -> "Double"
                PrimitiveKind.BOOLEAN -> "Boolean"
            },
        )
        is SwaggerType.ModelRef -> {
            val pkg = if (forDomain) domainModelPackage else dataModelPackage
            val simpleName = if (forDomain) domainModelName(type.name) else dataModelName(type.name)
            CodeRef(simpleName, setOf("$pkg.$simpleName"))
        }
        is SwaggerType.ListType -> typeRef(type.elementType, forDomain).let { inner ->
            CodeRef("List<${inner.code}>", inner.imports)
        }
        is SwaggerType.MapType -> typeRef(type.valueType, forDomain).let { value ->
            CodeRef("Map<String, ${value.code}>", value.imports)
        }
        SwaggerType.Unknown -> CodeRef("JsonElement", setOf("kotlinx.serialization.json.JsonElement"))
    }

    fun serviceReturnType(responseType: SwaggerType?): CodeRef {
        val bodyType = typeRef(responseType ?: SwaggerType.Unknown, forDomain = false)
        if (!hasResultWrappers()) return bodyType
        val apiResult = template.apiResultClass.orEmpty()
        val commonResult = template.commonResultClass.orEmpty()
        return CodeRef(
            "${apiResult.substringAfterLast(".")}<${commonResult.substringAfterLast(".")}<${bodyType.code}>>",
            bodyType.imports + apiResult + commonResult,
        )
    }

    fun repositoryReturnType(responseType: SwaggerType?): CodeRef {
        val bodyType = typeRef(responseType ?: SwaggerType.Unknown, forDomain = true)
        val resultClass = template.baseClassPackages.resultClass
        if (resultClass != null && hasResultWrappers()) {
            return CodeRef("${resultClass.substringAfterLast(".")}<${bodyType.code}>", bodyType.imports + resultClass)
        }
        return bodyType
    }

    fun operationModel(operation: SwaggerOperation, serviceTypes: Boolean): Map<String, Any> {
        val bodyType = operation.requestBody?.let { typeRef(it, forDomain = !serviceTypes).code }
        return mapOf(
            "operationId" to operation.operationId.toSafeIdentifier(),
            "methodAnnotationSimple" to operation.method.uppercase().toRetrofitMethod(),
            "path" to operation.path,
            "params" to operationParams(operation),
            "hasBody" to (operation.requestBody != null),
            "bodyType" to (bodyType ?: ""),
            "returnType" to if (serviceTypes) serviceReturnType(operation.responseBody).code else repositoryReturnType(operation.responseBody).code,
        )
    }

    private fun operationParams(operation: SwaggerOperation): List<Map<String, Any>> =
        operation.params.map { parameter ->
            val paramType = typeRef(parameter.type, forDomain = false).code + if (!parameter.required) "?" else ""
            mapOf(
                "name" to parameter.name.toSafeIdentifier(),
                "originalName" to parameter.originalName,
                "pathAnnotation" to (parameter.location.lowercase() == "path"),
                "type" to paramType,
            )
        }

    fun repositoryStatement(operation: SwaggerOperation): String {
        val callArgs = buildList {
            addAll(operation.params.map { it.name.toSafeIdentifier() })
            if (operation.requestBody != null) add("body.toData()")
        }.joinToString(", ")
        val serviceCall = "service.${operation.operationId.toSafeIdentifier()}($callArgs)"
        val mapperExpr = repositoryResponseMapExpression(operation.responseBody, "it")
        return if (hasResultWrappers()) {
            if (mapperExpr != null) {
                "return $serviceCall.toResult { $mapperExpr }"
            } else {
                "return $serviceCall.toResult()"
            }
        } else {
            repositoryResponseMapExpression(operation.responseBody, serviceCall)?.let { "return $it" }
                ?: "return $serviceCall"
        }
    }

    fun toDomainExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String =
        mapExpression(type, sourceExpr, nullableContainer, "toDomain")

    fun toDataExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String =
        mapExpression(type, sourceExpr, nullableContainer, "toData")

    fun repositoryResponseMapExpression(type: SwaggerType?, sourceExpr: String): String? =
        when (type) {
            null,
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
                -> null
            is SwaggerType.ModelRef,
            is SwaggerType.ListType,
            is SwaggerType.MapType,
                -> mapExpressionNonNull(type, sourceExpr, "toDomain")
        }

    fun requiresToDomainImport(type: SwaggerType?): Boolean =
        when (type) {
            is SwaggerType.ModelRef -> true
            is SwaggerType.ListType -> requiresToDomainImport(type.elementType)
            is SwaggerType.MapType -> requiresToDomainImport(type.valueType)
            null,
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
                -> false
        }

    fun moduleModel(): Map<String, Any> =
        mapOf(
            "rootPackage" to rootPackage,
            "dataPackage" to dataPackage,
            "domainPackage" to domainPackage,
            "generateDiPackage" to generateDiPackage,
            "dataRepositoryPackage" to dataRepositoryPackage,
            "domainRepositoryPackage" to domainRepositoryPackage,
            "servicePackage" to servicePackage,
            "serviceName" to serviceName,
            "repositoryImplName" to repositoryImplName,
            "apiRepositorySupportName" to repositoryImplName,
            "apiRepositoryName" to repositoryName,
            "repositoryName" to repositoryName,
            "pascalModuleName" to pascalModuleName,
        )

    fun repositoryModel(operations: List<Map<String, Any>>, imports: List<String>): Map<String, Any> =
        moduleModel() + mapOf(
            "packageName" to domainRepositoryPackage,
            "apiRepositoryName" to repositoryName,
            "repositoryName" to repositoryName,
            "operations" to operations,
            "imports" to imports,
        )

    fun useCaseModel(operation: SwaggerOperation): Map<String, Any> {
        val operations = operationModel(operation, serviceTypes = false)
        val imports = buildSet {
            add("${domainRepositoryPackage}.${repositoryName}")
            operation.requestBody?.let { addAll(typeRef(it, forDomain = true).imports) }
            addAll(repositoryReturnType(operation.responseBody).imports)
        }.filterImports(domainUseCasePackage)
        val params = operationParams(operation)
        val callArgs = buildList {
            addAll(params.map { it["name"] as String })
            if (operation.requestBody != null) add("body")
        }.joinToString(", ")
        return mapOf(
            "useCaseClass" to "${operation.operationId.toSafePascal()}UseCase",
            "useCaseName" to "${operation.operationId.toSafePascal()}UseCase",
            "domainUseCasePackage" to domainUseCasePackage,
            "repositoryName" to repositoryName,
            "params" to params,
            "hasBody" to (operation.requestBody != null),
            "bodyType" to (operations["bodyType"] ?: ""),
            "returnType" to operations["returnType"].orEmptyString(),
            "imports" to imports,
            "callArgs" to callArgs,
        )
    }

    private fun mapExpressionNonNull(type: SwaggerType, sourceExpr: String, method: String): String =
        when (type) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
                -> sourceExpr
            is SwaggerType.ModelRef -> "$sourceExpr.$method()"
            is SwaggerType.ListType -> "$sourceExpr.map { ${mapExpressionNonNull(type.elementType, "it", method)} }"
            is SwaggerType.MapType -> "$sourceExpr.mapValues { (_, value) -> ${mapExpressionNonNull(type.valueType, "value", method)} }"
        }

    private fun mapExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean, method: String): String {
        val mappedExpr = mapExpressionNonNull(type, sourceExpr, method)
        if (!nullableContainer) return mappedExpr
        return when (type) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
                -> sourceExpr
            is SwaggerType.ModelRef -> "$sourceExpr?.$method()"
            is SwaggerType.ListType -> "$sourceExpr?.map { ${mapExpressionNonNull(type.elementType, "it", method)} }"
            is SwaggerType.MapType -> "$sourceExpr?.mapValues { (_, value) -> ${mapExpressionNonNull(type.valueType, "value", method)} }"
        }
    }
}

private data class CodeRef(
    val code: String,
    val imports: Set<String> = emptySet(),
)

private fun SwaggerSchema.isCommonResultWrapper(): Boolean {
    val originalNames = properties.map { it.originalName }.toSet()
    return originalNames.contains("code") && originalNames.contains("msg") && originalNames.contains("data")
}

private fun SwaggerType?.unwrap(wrapperMap: Map<String, SwaggerType?>): SwaggerType? =
    if (this is SwaggerType.ModelRef && name in wrapperMap) wrapperMap[name] ?: this else this

private fun SwaggerSpec.collectRequestSchemaNames(): Set<String> {
    val names = mutableSetOf<String>()
    fun collect(type: SwaggerType?) {
        when (type) {
            is SwaggerType.ModelRef -> names.add(type.name)
            is SwaggerType.ListType -> collect(type.elementType)
            is SwaggerType.MapType -> collect(type.valueType)
            else -> Unit
        }
    }
    operations.forEach { collect(it.requestBody) }
    return names
}

private fun Set<String>.filterImports(currentPackage: String): List<String> =
    filter { it.isNotBlank() && it.substringBeforeLast(".", missingDelimiterValue = "") != currentPackage }
        .distinct()
        .sorted()

private fun String.toRetrofitMethod(): String =
    when (uppercase()) {
        "GET", "POST", "PUT", "DELETE", "PATCH" -> uppercase()
        else -> "GET"
    }

private fun String.toSafePascal(): String =
    replace(Regex("[^A-Za-z0-9]"), " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString("") { part -> part.replaceFirstChar { c -> c.uppercase() } }
        .ifBlank { "AutoGen" }

private fun String.toSafeIdentifier(): String {
    val id = replace(Regex("[^A-Za-z0-9_]"), "_")
    val headSafe = if (id.firstOrNull()?.isDigit() == true) "_$id" else id
    return if (headSafe in setOf("in", "class", "object", "when", "is", "fun")) "${headSafe}Value" else headSafe
}

private fun String.toPath(): String = replace('.', '/')

private fun Any?.orEmptyString(): String = this?.toString().orEmpty()
