package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.data.ModuleGenerator
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.slf4j.LoggerFactory

class SwaggerCodeGenerator {
    private val logger = LoggerFactory.getLogger(SwaggerCodeGenerator::class.java)

    fun generate(template: ModuleTemplate, spec: SwaggerSpec): List<ModuleGenerator.GeneratedFile> {
        val moduleDir = "feature/${template.name}"
        val files = mutableListOf<ModuleGenerator.GeneratedFile>()
        val ctx = GeneratorContext(template)

        val (wrapperSchemas, dataSchemas) = spec.schemas.partition { isCommonResultWrapper(it) }
        val wrapperUnwrapMap = wrapperSchemas.associate { schema ->
            schema.name to schema.properties.firstOrNull { it.originalName == "data" }?.type
        }

        for (schema in dataSchemas) {
            files.addKt(moduleDir, generateDataModel(schema, ctx))
            files.addKt(moduleDir, generateDomainModel(schema, ctx))
        }

        val adjustedSpec = spec.copy(
            operations = spec.operations.map { op ->
                op.copy(
                    params = op.params.filter { it.location.lowercase() != "header" },
                    responseBody = unwrapResponseBody(op.responseBody, wrapperUnwrapMap),
                )
            },
        )

        files.addKt(moduleDir, generateServiceInterface(adjustedSpec, ctx))
        files.addKt(moduleDir, generateRepositoryInterface(adjustedSpec, ctx))
        files.addKt(moduleDir, generateRepositoryImpl(adjustedSpec, ctx))
        files.addKt(moduleDir, generateDataModule(ctx))
        files.addKt(moduleDir, generateDomainModule(adjustedSpec, ctx))
        files.addKt(moduleDir, generateRootKoinModule(ctx))

        adjustedSpec.operations.forEach { op ->
            files.addKt(moduleDir, generateUseCase(op, ctx))
        }

        logger.info("Generated ${files.size} swagger scaffold files for module ${template.name}")
        return files
    }

    private fun isCommonResultWrapper(schema: SwaggerSchema): Boolean {
        val originalNames = schema.properties.map { it.originalName }.toSet()
        return originalNames.contains("code") && originalNames.contains("msg") && originalNames.contains("data")
    }

    private fun unwrapResponseBody(
        responseType: SwaggerType?,
        wrapperMap: Map<String, SwaggerType?>,
    ): SwaggerType? {
        if (responseType is SwaggerType.ModelRef && responseType.name in wrapperMap) {
            return wrapperMap[responseType.name] ?: responseType
        }
        return responseType
    }

    private fun generateDataModel(schema: SwaggerSchema, ctx: GeneratorContext): FileSpec {
        val className = ctx.dataModelName(schema.name)
        val dataClassName = ClassName(ctx.dataModelPackage, className)
        val domainClassName = ClassName(ctx.domainModelPackage, ctx.domainModelName(schema.name))
        val serialNameClass = ClassName("kotlinx.serialization", "SerialName")
        val typeBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    schema.properties.forEach { prop ->
                        val type = ctx.resolveType(prop.type, forDomain = false)
                            .copy(nullable = !prop.required)
                        val paramBuilder = ParameterSpec.builder(prop.name, type)
                            .addAnnotation(
                                AnnotationSpec.builder(serialNameClass)
                                    .addMember("%S", prop.originalName)
                                    .build(),
                            )
                        if (!prop.required) paramBuilder.defaultValue("null")
                        addParameter(paramBuilder.build())
                    }
                }.build(),
            )

        schema.properties.forEach { prop ->
            val type = ctx.resolveType(prop.type, forDomain = false)
                .copy(nullable = !prop.required)
            typeBuilder.addProperty(
                PropertySpec.builder(prop.name, type)
                    .initializer(prop.name)
                    .build(),
            )
        }

        val mapperBody = CodeBlock.builder()
            .add("return %T(\n", domainClassName)
            .indent()
            .apply {
                schema.properties.forEach { prop ->
                    add(
                        "%L = %L,\n",
                        prop.name,
                        ctx.toDomainExpression(prop.type, "this.${prop.name}", !prop.required),
                    )
                }
            }
            .unindent()
            .add(")\n")
            .build()

        val mapperFun = FunSpec.builder("toDomain")
            .receiver(dataClassName)
            .addModifiers(KModifier.INTERNAL)
            .returns(domainClassName)
            .addCode(mapperBody)
            .build()

        return FileSpec.builder(ctx.dataModelPackage, className)
            .addType(typeBuilder.build())
            .addFunction(mapperFun)
            .build()
    }

    private fun generateDomainModel(schema: SwaggerSchema, ctx: GeneratorContext): FileSpec {
        val className = ctx.domainModelName(schema.name)
        val typeBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    schema.properties.forEach { prop ->
                        addParameter(buildModelParameter(prop, ctx, forDomain = true))
                    }
                }.build(),
            )

        schema.properties.forEach { prop ->
            val type = ctx.resolveType(prop.type, forDomain = true)
                .copy(nullable = !prop.required)
            typeBuilder.addProperty(
                PropertySpec.builder(prop.name, type)
                    .initializer(prop.name)
                    .build(),
            )
        }

        return FileSpec.builder(ctx.domainModelPackage, className)
            .addType(typeBuilder.build())
            .build()
    }

    private fun buildModelParameter(
        prop: SwaggerProperty,
        ctx: GeneratorContext,
        forDomain: Boolean,
    ): ParameterSpec {
        val type = ctx.resolveType(prop.type, forDomain).copy(nullable = !prop.required)
        val builder = ParameterSpec.builder(prop.name, type)
        if (!prop.required) builder.defaultValue("null")
        return builder.build()
    }

    private fun generateServiceInterface(spec: SwaggerSpec, ctx: GeneratorContext): FileSpec {
        val typeBuilder = TypeSpec.interfaceBuilder(ctx.serviceName)
            .addModifiers(KModifier.INTERNAL)

        spec.operations.forEach { op ->
            val funBuilder = FunSpec.builder(op.operationId)
                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                .addAnnotation(
                    AnnotationSpec.builder(ctx.retrofitMethodAnnotation(op.method))
                        .addMember("%S", op.path)
                        .build(),
                )
                .returns(ctx.serviceReturnType(op.responseBody))

            op.params.forEach { param ->
                funBuilder.addParameter(ctx.buildServiceParameter(param))
            }
            op.requestBody?.let { bodyType ->
                funBuilder.addParameter(
                    ParameterSpec.builder("body", ctx.resolveType(bodyType, forDomain = false))
                        .addAnnotation(AnnotationSpec.builder(ClassName("retrofit2.http", "Body")).build())
                        .build(),
                )
            }

            typeBuilder.addFunction(funBuilder.build())
        }

        return FileSpec.builder(ctx.servicePackage, ctx.serviceName)
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateRepositoryInterface(spec: SwaggerSpec, ctx: GeneratorContext): FileSpec {
        val typeBuilder = TypeSpec.interfaceBuilder(ctx.repositoryName)
            .addModifiers(KModifier.INTERNAL)

        spec.operations.forEach { op ->
            val funBuilder = FunSpec.builder(op.operationId)
                .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                .returns(ctx.repositoryReturnType(op.responseBody))
            op.params.forEach { param ->
                funBuilder.addParameter(ctx.buildRepositoryParam(param))
            }
            op.requestBody?.let { bodyType ->
                funBuilder.addParameter("body", ctx.resolveType(bodyType, forDomain = false))
            }
            typeBuilder.addFunction(funBuilder.build())
        }

        return FileSpec.builder(ctx.domainRepositoryPackage, ctx.repositoryName)
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateRepositoryImpl(spec: SwaggerSpec, ctx: GeneratorContext): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(ctx.repositoryImplName)
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(ClassName(ctx.domainRepositoryPackage, ctx.repositoryName))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("service", ClassName(ctx.servicePackage, ctx.serviceName))
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("service", ClassName(ctx.servicePackage, ctx.serviceName))
                    .initializer("service")
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )

        spec.operations.forEach { op ->
            val funBuilder = FunSpec.builder(op.operationId)
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .returns(ctx.repositoryReturnType(op.responseBody))

            val callArgs = mutableListOf<String>()
            op.params.forEach { param ->
                val paramName = param.name.toSafeIdentifier()
                funBuilder.addParameter(ctx.buildRepositoryParam(param))
                callArgs.add(paramName)
            }
            op.requestBody?.let { bodyType ->
                funBuilder.addParameter("body", ctx.resolveType(bodyType, forDomain = false))
                callArgs.add("body")
            }

            val serviceCall = "service.${op.operationId}(${callArgs.joinToString(", ")})"
            val mapperExpr = ctx.repositoryResponseMapExpression(op.responseBody, "it")
            if (ctx.hasResultWrappers()) {
                val toResult = MemberName(ctx.template.toResultPackage ?: "", "toResult")
                if (mapperExpr != null) {
                    funBuilder.addStatement("return %L.%M { %L }", serviceCall, toResult, mapperExpr)
                } else {
                    funBuilder.addStatement("return %L.%M()", serviceCall, toResult)
                }
            } else {
                if (mapperExpr != null) {
                    val mappedServiceCall = ctx.repositoryResponseMapExpression(op.responseBody, serviceCall)
                    funBuilder.addStatement("return %L", mappedServiceCall!!)
                } else {
                    funBuilder.addStatement("return %L", serviceCall)
                }
            }
            typeBuilder.addFunction(funBuilder.build())
        }

        val needsToDomainImport = spec.operations.any { ctx.requiresToDomainImport(it.responseBody) }
        return FileSpec.builder(ctx.dataRepositoryPackage, ctx.repositoryImplName)
            .apply {
                if (needsToDomainImport) {
                    addImport(ctx.dataModelPackage, "toDomain")
                }
            }
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateDataModule(ctx: GeneratorContext): FileSpec {
        val moduleClass = ClassName("org.koin.core.module", "Module")
        val repoImpl = ClassName(ctx.dataRepositoryPackage, ctx.repositoryImplName)
        val repoInterface = ClassName(ctx.domainRepositoryPackage, ctx.repositoryName)
        val service = ClassName(ctx.servicePackage, ctx.serviceName)

        val body = CodeBlock.builder()
            .beginControlFlow("%M", MemberName("org.koin.dsl", "module"))
            .addStatement("%M(::%T) { %M<%T>() }",
                MemberName("org.koin.core.module.dsl", "singleOf"),
                repoImpl,
                MemberName("org.koin.core.module.dsl", "bind"),
                repoInterface)
            .addStatement("single { get<%T>().create(%T::class.java) }",
                ClassName("retrofit2", "Retrofit"),
                service)
            .endControlFlow()
            .build()

        return FileSpec.builder(ctx.dataPackage, "dataModule")
            .addProperty(
                PropertySpec.builder("dataModule", moduleClass)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer(body)
                    .build(),
            )
            .build()
    }

    private fun generateDomainModule(spec: SwaggerSpec, ctx: GeneratorContext): FileSpec {
        val moduleClass = ClassName("org.koin.core.module", "Module")
        val body = CodeBlock.builder()
            .beginControlFlow("%M", MemberName("org.koin.dsl", "module"))
        spec.operations.forEach { op ->
            body.addStatement("%M(::%T)",
                MemberName("org.koin.core.module.dsl", "singleOf"),
                ClassName(ctx.domainUseCasePackage, "${op.operationId.toSafePascal()}UseCase"))
        }
        body.endControlFlow()

        return FileSpec.builder(ctx.domainPackage, "domainModule")
            .addProperty(
                PropertySpec.builder("domainModule", moduleClass)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer(body.build())
                    .build(),
            )
            .build()
    }

    private fun generateRootKoinModule(ctx: GeneratorContext): FileSpec {
        val moduleClass = ClassName("org.koin.core.module", "Module")
        val listType = ClassName("kotlin.collections", "List").parameterizedBy(moduleClass)

        return FileSpec.builder(ctx.rootPackage, "${ctx.pascalModuleName}KoinModule")
            .addProperty(
                PropertySpec.builder("feature${ctx.pascalModuleName}Modules", listType)
                    .initializer(
                        CodeBlock.of(
                            "listOf(%T, %T)",
                            ClassName(ctx.domainPackage, "domainModule"),
                            ClassName(ctx.dataPackage, "dataModule"),
                        ),
                    )
                    .build(),
            )
            .build()
    }

    private fun generateUseCase(op: SwaggerOperation, ctx: GeneratorContext): FileSpec {
        val useCaseName = "${op.operationId.toSafePascal()}UseCase"
        val repositoryClass = ClassName(ctx.domainRepositoryPackage, ctx.repositoryName)

        val typeBuilder = TypeSpec.classBuilder(useCaseName)
            .addModifiers(KModifier.INTERNAL)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("repository", repositoryClass)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("repository", repositoryClass)
                    .initializer("repository")
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )

        val invokeBuilder = FunSpec.builder("invoke")
            .addModifiers(KModifier.OPERATOR, KModifier.SUSPEND)
            .returns(ctx.repositoryReturnType(op.responseBody))

        val args = mutableListOf<String>()
        op.params.forEach { param ->
            invokeBuilder.addParameter(ctx.buildRepositoryParam(param))
            args.add(param.name.toSafeIdentifier())
        }
        op.requestBody?.let { bodyType ->
            invokeBuilder.addParameter("body", ctx.resolveType(bodyType, forDomain = false))
            args.add("body")
        }
        invokeBuilder.addStatement("return repository.%L(%L)", op.operationId, args.joinToString(", "))
        typeBuilder.addFunction(invokeBuilder.build())

        return FileSpec.builder(ctx.domainUseCasePackage, useCaseName)
            .addType(typeBuilder.build())
            .build()
    }

    private fun MutableList<ModuleGenerator.GeneratedFile>.addKt(moduleDir: String, fileSpec: FileSpec) {
        val pkgPath = fileSpec.packageName.replace('.', '/')
        val path = "$moduleDir/src/main/kotlin/$pkgPath/${fileSpec.name}.kt"
        val content = fileSpec.toString()
            .replace("`data`", "data")
            .replace(Regex("""(?m)^(\s*)public """), "$1")
        add(ModuleGenerator.GeneratedFile(path, content))
    }
}

private class GeneratorContext(val template: ModuleTemplate) {
    val moduleName = template.name
    val pascalModuleName = moduleName.toSafePascal()
    val rootPackage = template.packageName
    val dataPackage = "$rootPackage.data"
    val domainPackage = "$rootPackage.domain"
    val dataModelPackage = "$dataPackage.datasource.api.model"
    val servicePackage = "$dataPackage.datasource.api.service"
    val dataRepositoryPackage = "$dataPackage.repository"
    val domainModelPackage = "$domainPackage.model"
    val domainRepositoryPackage = "$domainPackage.repository"
    val domainUseCasePackage = "$domainPackage.usecase"
    val serviceName = "${pascalModuleName}RetrofitService"
    val repositoryName = "${pascalModuleName}Repository"
    val repositoryImplName = "${pascalModuleName}RepositoryImpl"

    fun dataModelName(rawName: String): String = "${rawName.toSafePascal()}ApiModel"
    fun domainModelName(rawName: String): String = rawName.toSafePascal()

    fun hasResultWrappers(): Boolean =
        template.apiResultClass != null && template.commonResultClass != null && template.toResultPackage != null

    fun serviceReturnType(responseType: SwaggerType?): TypeName {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = false)
        if (!hasResultWrappers()) return bodyType
        val common = (template.commonResultClass ?: "").toClassName().parameterizedBy(bodyType)
        return (template.apiResultClass ?: "").toClassName().parameterizedBy(common)
    }

    fun repositoryReturnType(responseType: SwaggerType?): TypeName {
        val bodyType = resolveType(responseType ?: SwaggerType.Unknown, forDomain = true)
        if (template.baseClassPackages.resultClass != null && hasResultWrappers()) {
            return template.baseClassPackages.resultClass.toClassName().parameterizedBy(bodyType)
        }
        return bodyType
    }

    fun resolveType(type: SwaggerType, forDomain: Boolean): TypeName = when (type) {
        is SwaggerType.Primitive -> when (type.kind) {
            PrimitiveKind.STRING -> ClassName("kotlin", "String")
            PrimitiveKind.INT -> ClassName("kotlin", "Int")
            PrimitiveKind.LONG -> ClassName("kotlin", "Long")
            PrimitiveKind.DOUBLE -> ClassName("kotlin", "Double")
            PrimitiveKind.BOOLEAN -> ClassName("kotlin", "Boolean")
        }
        is SwaggerType.ModelRef -> {
            val pkg = if (forDomain) domainModelPackage else dataModelPackage
            val simpleName = if (forDomain) domainModelName(type.name) else dataModelName(type.name)
            ClassName(pkg, simpleName)
        }
        is SwaggerType.ListType -> ClassName("kotlin.collections", "List").parameterizedBy(
            resolveType(type.elementType, forDomain),
        )
        is SwaggerType.MapType -> ClassName("kotlin.collections", "Map").parameterizedBy(
            ClassName("kotlin", "String"),
            resolveType(type.valueType, forDomain),
        )
        SwaggerType.Unknown -> ClassName("kotlinx.serialization.json", "JsonElement")
    }

    fun retrofitMethodAnnotation(method: String): ClassName = when (method.uppercase()) {
        "GET" -> ClassName("retrofit2.http", "GET")
        "POST" -> ClassName("retrofit2.http", "POST")
        "PUT" -> ClassName("retrofit2.http", "PUT")
        "DELETE" -> ClassName("retrofit2.http", "DELETE")
        "PATCH" -> ClassName("retrofit2.http", "PATCH")
        else -> ClassName("retrofit2.http", "GET")
    }

    fun buildServiceParameter(param: SwaggerParameter): ParameterSpec {
        val type = resolveType(param.type, forDomain = false)
            .copy(nullable = !param.required)
        val ann = when (param.location.lowercase()) {
            "path" -> AnnotationSpec.builder(ClassName("retrofit2.http", "Path"))
            "header" -> AnnotationSpec.builder(ClassName("retrofit2.http", "Header"))
            else -> AnnotationSpec.builder(ClassName("retrofit2.http", "Query"))
        }.addMember("%S", param.originalName).build()
        return ParameterSpec.builder(param.name.toSafeIdentifier(), type)
            .addAnnotation(ann)
            .build()
    }

    fun buildRepositoryParam(param: SwaggerParameter): ParameterSpec =
        ParameterSpec.builder(
            param.name.toSafeIdentifier(),
            resolveType(param.type, forDomain = false).copy(nullable = !param.required),
        ).build()

    fun toDomainExpression(type: SwaggerType, sourceExpr: String, nullableContainer: Boolean): String {
        val mappedExpr = toDomainNonNullExpression(type, sourceExpr)
        if (!nullableContainer) return mappedExpr
        return when (type) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> sourceExpr
            is SwaggerType.ModelRef -> "$sourceExpr?.toDomain()"
            is SwaggerType.ListType -> "$sourceExpr?.map { ${toDomainNonNullExpression(type.elementType, "it")} }"
            is SwaggerType.MapType ->
                "$sourceExpr?.mapValues { (_, value) -> ${toDomainNonNullExpression(type.valueType, "value")} }"
        }
    }

    private fun toDomainNonNullExpression(type: SwaggerType, sourceExpr: String): String = when (type) {
        is SwaggerType.Primitive,
        SwaggerType.Unknown,
        -> sourceExpr
        is SwaggerType.ModelRef -> "$sourceExpr.toDomain()"
        is SwaggerType.ListType -> "$sourceExpr.map { ${toDomainNonNullExpression(type.elementType, "it")} }"
        is SwaggerType.MapType ->
            "$sourceExpr.mapValues { (_, value) -> ${toDomainNonNullExpression(type.valueType, "value")} }"
    }

    fun repositoryResponseMapExpression(type: SwaggerType?, sourceExpr: String): String? {
        val t = type ?: return null
        return when (t) {
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> null
            is SwaggerType.ModelRef,
            is SwaggerType.ListType,
            is SwaggerType.MapType,
            -> toDomainNonNullExpression(t, sourceExpr)
        }
    }

    fun requiresToDomainImport(type: SwaggerType?): Boolean {
        val t = type ?: return false
        return when (t) {
            is SwaggerType.ModelRef -> true
            is SwaggerType.ListType -> requiresToDomainImport(t.elementType)
            is SwaggerType.MapType -> requiresToDomainImport(t.valueType)
            is SwaggerType.Primitive,
            SwaggerType.Unknown,
            -> false
        }
    }
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

private fun String.toClassName(): ClassName {
    val p = split(".")
    return ClassName(p.dropLast(1).joinToString("."), p.last())
}
