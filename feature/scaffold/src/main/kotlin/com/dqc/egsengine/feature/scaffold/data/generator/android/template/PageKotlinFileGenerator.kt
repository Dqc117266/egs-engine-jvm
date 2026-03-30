package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName

class PageKotlinFileGenerator(private val template: PageTemplate) {

    private val pascalName = template.pageName
    private val camelName = pascalName.replaceFirstChar { it.lowercase() }
    private val layoutSnakeName = pascalName.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    private val pkg = template.modulePackage
    private val screenPkg = "$pkg.presentation.screen.$camelName"
    private val baseClasses = template.baseClassPackages
    private val basePackage = template.basePackage

    private val uiStateClass = baseClasses.baseViewModel?.let {
        ClassName(it.substringBeforeLast("."), "UiState")
    } ?: ClassName("com.example.base", "UiState")

    private val uiIntentClass = baseClasses.baseViewModel?.let {
        ClassName(it.substringBeforeLast("."), "UiIntent")
    } ?: ClassName("com.example.base", "UiIntent")

    private val uiEffectClass = baseClasses.baseViewModel?.let {
        ClassName(it.substringBeforeLast("."), "UiEffect")
    } ?: ClassName("com.example.base", "UiEffect")

    private val baseViewModelClass = baseClasses.baseViewModel?.let {
        ClassName(it.substringBeforeLast("."), it.substringAfterLast("."))
    } ?: ClassName("androidx.lifecycle", "ViewModel")

    private val baseFragmentClass = baseClasses.baseFragment?.let {
        ClassName(it.substringBeforeLast("."), it.substringAfterLast("."))
    }

    fun generateContract(): FileSpec {
        val intentClass = ClassName(screenPkg, "${pascalName}Contract", "Intent")
        val effectClass = ClassName(screenPkg, "${pascalName}Contract", "Effect")

        val modelPackage = "$pkg.domain.model"
        val stateBuilder = TypeSpec.classBuilder("State")
            .addModifiers(KModifier.DATA)
            .addSuperinterface(uiStateClass)
        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("isLoading", Boolean::class).defaultValue("false").build())
            .addParameter(
                ParameterSpec.builder("error", String::class.asTypeName().copy(nullable = true))
                    .defaultValue("null")
                    .build(),
            )
        val stateProperties = mutableListOf<PropertySpec>()
        stateProperties.add(PropertySpec.builder("isLoading", Boolean::class).initializer("isLoading").build())
        stateProperties.add(
            PropertySpec.builder("error", String::class.asTypeName().copy(nullable = true))
                .initializer("error")
                .build(),
        )

        template.useCases.forEach { useCase ->
            val propName = useCase.camelName
            val returnType = useCase.returnType
            if (returnType != null) {
                val innerType = Regex("""Result<([^>]+)>""").find(returnType)?.groupValues?.get(1) ?: returnType
                val simpleType = innerType.substringAfterLast(".")
                val typePackage = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
                val typeClass = resolveStatePropertyType(simpleType, typePackage, modelPackage)
                constructorBuilder.addParameter(
                    ParameterSpec.builder(propName, typeClass.copy(nullable = true))
                        .defaultValue("null")
                        .build(),
                )
                stateProperties.add(PropertySpec.builder(propName, typeClass.copy(nullable = true)).initializer(propName).build())
            }
        }

        val stateType = stateBuilder
            .primaryConstructor(constructorBuilder.build())
            .addProperties(stateProperties)
            .build()

        val intentType = TypeSpec.classBuilder("Intent")
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(uiIntentClass)
            .addTypes(
                template.useCases.map { useCase ->
                    buildIntentType(useCase, intentClass)
                },
            )
            .build()

        val effectType = TypeSpec.classBuilder("Effect")
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(uiEffectClass)
            .addType(
                TypeSpec.classBuilder("ShowToast")
                    .addModifiers(KModifier.DATA)
                    .superclass(effectClass)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("message", String::class)
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder("message", String::class)
                            .initializer("message")
                            .build(),
                    )
                    .build(),
            )
            .build()

        return FileSpec.builder(screenPkg, "${pascalName}Contract")
            .addType(
                TypeSpec.interfaceBuilder("${pascalName}Contract")
                    .addType(stateType)
                    .addType(intentType)
                    .addType(effectType)
                    .build(),
            )
            .build()
    }

    private fun resolveStatePropertyType(
        simpleType: String,
        typePackage: String,
        modelPackage: String,
    ): com.squareup.kotlinpoet.TypeName {
        if (simpleType.startsWith("List<") && simpleType.endsWith(">")) {
            val innerType = simpleType.substring(5, simpleType.length - 1)
            val innerSimpleType = innerType.substringAfterLast(".")
            val innerTypePackage = if (innerType.contains(".")) innerType.substringBeforeLast(".") else modelPackage
            val innerClass = resolveBasicType(innerSimpleType, innerTypePackage, modelPackage)
            return ClassName("kotlin.collections", "List").parameterizedBy(innerClass)
        }

        return resolveBasicType(simpleType, typePackage, modelPackage)
    }

    private fun resolveBasicType(
        simpleType: String,
        typePackage: String,
        modelPackage: String,
    ): com.squareup.kotlinpoet.TypeName = when (simpleType) {
        "Boolean", "ModelBoolean", "KotlinBoolean" -> Boolean::class.asTypeName()
        "Int", "ModelInt", "KotlinInt" -> Int::class.asTypeName()
        "Long", "ModelLong", "KotlinLong" -> Long::class.asTypeName()
        "String", "ModelString", "KotlinString" -> String::class.asTypeName()
        "Double", "ModelDouble", "KotlinDouble" -> Double::class.asTypeName()
        "Float", "ModelFloat", "KotlinFloat" -> Float::class.asTypeName()
        else -> {
            if (simpleType.endsWith("ApiModel")) {
                ClassName(modelPackage, simpleType.removeSuffix("ApiModel"))
            } else {
                ClassName(typePackage, simpleType)
            }
        }
    }

    private fun buildIntentType(useCase: UseCaseInfo, intentClass: ClassName): TypeSpec {
        val intentName = useCase.name.removeSuffix("UseCase")
        return if (useCase.parameters.isEmpty()) {
            TypeSpec.objectBuilder(intentName)
                .superclass(intentClass)
                .build()
        } else {
            val modelPackage = "$pkg.domain.model"
            val constructorBuilder = FunSpec.constructorBuilder()
            val properties = mutableListOf<PropertySpec>()
            useCase.parameters.forEach { param ->
                val paramType = resolveParamType(param.type, modelPackage)
                constructorBuilder.addParameter(param.name, paramType)
                properties.add(PropertySpec.builder(param.name, paramType).initializer(param.name).build())
            }
            TypeSpec.classBuilder(intentName)
                .addModifiers(KModifier.DATA)
                .superclass(intentClass)
                .primaryConstructor(constructorBuilder.build())
                .addProperties(properties)
                .build()
        }
    }

    private fun resolveParamType(typeStr: String, modelPackage: String): com.squareup.kotlinpoet.TypeName {
        val nullable = typeStr.endsWith("?")
        val base = typeStr.removeSuffix("?")
        val typeName = when {
            base.startsWith("List<") -> {
                val inner = Regex("""List<([^>]+)>""").find(base)?.groupValues?.get(1)
                    ?: return ClassName("kotlin.collections", "List")
                val innerType = resolveParamType(inner, modelPackage)
                ClassName("kotlin.collections", "List").parameterizedBy(innerType)
            }
            else -> base.split(".").let { parts ->
                val simple = parts.last()
                val pkg = if (parts.size > 1) parts.dropLast(1).joinToString(".") else modelPackage
                when (simple) {
                    "Boolean", "ModelBoolean", "KotlinBoolean" -> Boolean::class.asTypeName()
                    "Int", "ModelInt", "KotlinInt" -> Int::class.asTypeName()
                    "Long", "ModelLong", "KotlinLong" -> Long::class.asTypeName()
                    "String", "ModelString", "KotlinString" -> String::class.asTypeName()
                    "Double", "ModelDouble", "KotlinDouble" -> Double::class.asTypeName()
                    "Float", "ModelFloat", "KotlinFloat" -> Float::class.asTypeName()
                    else -> {
                        if (simple.endsWith("ApiModel")) {
                            ClassName(modelPackage, simple.removeSuffix("ApiModel"))
                        } else {
                            ClassName(pkg, simple)
                        }
                    }
                }
            }
        }
        return if (nullable) typeName.copy(nullable = true) else typeName
    }

    fun generateFragment(): FileSpec? {
        if (baseFragmentClass == null) return null

        val bindingClass = ClassName("$pkg.databinding", "Fragment${pascalName}Binding")
        val rClass = ClassName(pkg, "R")
        val stateClass = ClassName(screenPkg, "${pascalName}Contract.State")
        val effectClass = ClassName(screenPkg, "${pascalName}Contract.Effect")
        val viewModelClass = ClassName(screenPkg, "${pascalName}ViewModel")

        return FileSpec.builder(screenPkg, "${pascalName}Fragment")
            .addImport("androidx.lifecycle", "Lifecycle")
            .addImport("androidx.lifecycle", "lifecycleScope")
            .addImport("androidx.lifecycle", "repeatOnLifecycle")
            .addImport("kotlinx.coroutines", "launch")
            .addImport("org.koin.androidx.viewmodel.ext.android", "viewModel")
            .addType(
                TypeSpec.classBuilder("${pascalName}Fragment")
                    .superclass(baseFragmentClass.parameterizedBy(bindingClass))
                    .addSuperclassConstructorParameter("%T.layout.fragment_$layoutSnakeName", rClass)
                    .addProperty(
                        PropertySpec.builder("viewModel", viewModelClass, KModifier.PRIVATE)
                            .delegate("viewModel()")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("onViewCreated")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("view", ClassName("android.view", "View"))
                            .addParameter("savedInstanceState", ClassName("android.os", "Bundle").copy(nullable = true))
                            .addStatement("super.onViewCreated(view, savedInstanceState)")
                            .addStatement("")
                            .addStatement("initObserver()")
                            .build(),
                    )
                    .addFunction(generateInitObserverFun())
                    .addFunction(generateRenderStateFun(stateClass))
                    .addFunction(generateHandleEffectFun(effectClass))
                    .build(),
            )
            .build()
    }

    private fun generateInitObserverFun(): FunSpec =
        FunSpec.builder("initObserver")
            .addModifiers(KModifier.PRIVATE)
            .beginControlFlow("viewLifecycleOwner.lifecycleScope.launch")
            .beginControlFlow("viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)")
            .beginControlFlow("launch")
            .addStatement("viewModel.uiState.collect { state ->")
            .addStatement("    renderState(state)")
            .addStatement("}")
            .endControlFlow()
            .addStatement("")
            .beginControlFlow("launch")
            .addStatement("viewModel.effect.collect { effect ->")
            .addStatement("    handleEffect(effect)")
            .addStatement("}")
            .endControlFlow()
            .addStatement("")
            .beginControlFlow("launch")
            .addStatement("viewModel.isLoading.collect { isLoading ->")
            .addStatement("    // Handle loading state")
            .addStatement("}")
            .endControlFlow()
            .endControlFlow()
            .endControlFlow()
            .build()

    private fun generateRenderStateFun(stateClass: ClassName): FunSpec =
        FunSpec.builder("renderState")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("state", stateClass)
            .addStatement("binding.apply {")
            .addStatement("    // TODO: 绑定 UI")
            .addStatement("    // progressBar.isVisible = state.isLoading")
            .addStatement("    // errorView.isVisible = state.error != null")
            .addStatement("}")
            .build()

    private fun generateHandleEffectFun(effectClass: ClassName): FunSpec =
        FunSpec.builder("handleEffect")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("effect", effectClass)
            .beginControlFlow("when (effect)")
            .addStatement("is ${pascalName}Contract.Effect.ShowToast -> showToast(effect.message)")
            .endControlFlow()
            .build()

    fun generateViewModel(): FileSpec {
        val stateClass = ClassName(screenPkg, "${pascalName}Contract.State")
        val intentClass = ClassName(screenPkg, "${pascalName}Contract.Intent")
        val effectClass = ClassName(screenPkg, "${pascalName}Contract.Effect")

        val resultPackage = template.basePackage?.let { "$it.feature.base.domain.result" } ?: "com.example.feature.base.domain.result"
        val fileBuilder = FileSpec.builder(screenPkg, "${pascalName}ViewModel")
            .addImport(resultPackage, "Result")

        val classBuilder = TypeSpec.classBuilder("${pascalName}ViewModel")
            .addModifiers(KModifier.INTERNAL)

        if (template.useCases.isNotEmpty()) {
            val constructorBuilder = FunSpec.constructorBuilder()
            template.useCases.forEach { useCase ->
                val useCaseClass = ClassName(useCase.packageName, useCase.name)
                constructorBuilder.addParameter(useCase.camelName, useCaseClass)
            }
            classBuilder.primaryConstructor(constructorBuilder.build())

            template.useCases.forEach { useCase ->
                val useCaseClass = ClassName(useCase.packageName, useCase.name)
                classBuilder.addProperty(
                    PropertySpec.builder(useCase.camelName, useCaseClass)
                        .initializer(useCase.camelName)
                        .addModifiers(KModifier.PRIVATE)
                        .build(),
                )
            }
        }

        val baseVmWithTypes = if (baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            baseViewModelClass.parameterizedBy(stateClass, intentClass, effectClass)
        } else {
            baseViewModelClass
        }

        classBuilder.superclass(baseVmWithTypes)

        if (baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            classBuilder.addSuperclassConstructorParameter(
                CodeBlock.of("%T()", stateClass),
            )
        }

        val registerIntentsBuilder = FunSpec.builder("registerIntents")
            .addModifiers(KModifier.OVERRIDE)

        if (template.useCases.isNotEmpty() && baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            template.useCases.forEach { useCase ->
                val intentName = useCase.name.removeSuffix("UseCase")
                val handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}"
                registerIntentsBuilder
                    .addStatement("registerIntent<%T.%L> {", intentClass, intentName)
                if (useCase.parameters.isEmpty()) {
                    registerIntentsBuilder.addStatement("    %L()", handlerName)
                } else {
                    val paramArgs = useCase.parameters.joinToString(",\n                ") { "it.${it.name}" }
                    registerIntentsBuilder.addStatement("    %L(%L)", handlerName, paramArgs)
                }
                registerIntentsBuilder
                    .addStatement("}")
                    .addStatement("")
            }
        } else {
            registerIntentsBuilder.addStatement("// TODO: Register intent handlers")
        }

        classBuilder.addFunction(registerIntentsBuilder.build())

        template.useCases.forEach { useCase ->
            classBuilder.addFunction(generateHandleUseCaseFun(useCase, stateClass, effectClass))
        }

        return fileBuilder.addType(classBuilder.build()).build()
    }

    private fun generateHandleUseCaseFun(
        useCase: UseCaseInfo,
        stateClass: ClassName,
        @Suppress("UNUSED_PARAMETER") effectClass: ClassName,
    ): FunSpec {
        val intentName = useCase.name.removeSuffix("UseCase")
        val handlerName = "handle${intentName.replaceFirstChar { it.uppercase() }}"
        val showLoading = !(useCase.returnType?.contains("SseEmitter") == true)
        val resultPackage = template.basePackage?.let { "$it.feature.base.domain.result" } ?: "com.example.feature.base.domain.result"
        val resultClass = ClassName(resultPackage, "Result")
        val modelPackage = "$pkg.domain.model"

        val funBuilder = FunSpec.builder(handlerName)
            .addModifiers(KModifier.PRIVATE)

        useCase.parameters.forEach { param ->
            val paramType = resolveParamType(param.type, modelPackage)
            funBuilder.addParameter(param.name, paramType)
        }

        if (baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            funBuilder
                .beginControlFlow("launchRequest(showLoading = %L) {", showLoading)
            if (useCase.parameters.isEmpty()) {
                funBuilder
                    .beginControlFlow("when (val result = %L())", useCase.camelName)
            } else {
                val useCaseArgs = useCase.parameters.joinToString(", ") { "${it.name} = ${it.name}" }
                funBuilder
                    .beginControlFlow("when (val result = %L(%L))", useCase.camelName, useCaseArgs)
            }
            funBuilder
                .addStatement("is %T.Success -> {", resultClass)
                .addStatement("    updateState { copy(%L = result.value) }", useCase.camelName)
                .addStatement("}")
                .addStatement("is %T.Failure -> {", resultClass)
                .addStatement("    updateState { copy(error = result.throwable?.message) }")
                .addStatement("}")
                .endControlFlow()
                .endControlFlow()
        } else {
            funBuilder.addStatement("// TODO: 实现 %L 逻辑", useCase.camelName)
        }

        return funBuilder.build()
    }

    fun generateScreen(): FileSpec {
        val screenDirPkg = "$pkg.presentation.screen.$camelName"
        val viewModelClass = ClassName(screenDirPkg, "${pascalName}ViewModel")
        val effectClass = ClassName(screenDirPkg, "${pascalName}Contract", "Effect")
        val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
        val modifierClass = ClassName("androidx.compose.ui", "Modifier")

        return FileSpec.builder(screenDirPkg, "${pascalName}Screen")
            .addImport("androidx.compose.foundation.layout", "Column", "fillMaxSize", "padding")
            .addImport("androidx.compose.runtime", "LaunchedEffect", "getValue")
            .addImport("androidx.compose.ui", "Modifier")
            .addImport("androidx.compose.ui.unit", "dp")
            .addImport("androidx.lifecycle.compose", "collectAsStateWithLifecycle")
            .addImport("org.koin.androidx.compose", "koinViewModel")
            .addFunction(
                FunSpec.builder("${pascalName}Screen")
                    .addAnnotation(composableAnnotation)
                    .addParameter(
                        ParameterSpec.builder("modifier", modifierClass)
                            .defaultValue("%T", modifierClass)
                            .build(),
                    )
                    .addStatement("")
                    .addStatement("val viewModel: %T = koinViewModel()", viewModelClass)
                    .addStatement("val uiState by viewModel.uiState.collectAsStateWithLifecycle()")
                    .addStatement("")
                    .addComment("Handle effects (one-time events)")
                    .beginControlFlow("LaunchedEffect(Unit)")
                    .beginControlFlow("viewModel.effect.collect { effect ->")
                    .beginControlFlow("when (effect)")
                    .addStatement("is %T.ShowToast -> {", effectClass)
                    .addStatement("    // TODO: Show Toast via SnackbarHostState")
                    .addStatement("}")
                    .endControlFlow()
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("")
                    .addComment("UI Content")
                    .addStatement(
                        "Column(\n" +
                            "    modifier = modifier.fillMaxSize().padding(16.dp),\n" +
                            ") {",
                    )
                    .beginControlFlow("when")
                    .addStatement("uiState.isLoading -> {")
                    .addStatement("    // TODO: Show LoadingIndicator()")
                    .addStatement("}")
                    .addStatement("uiState.error != null -> {")
                    .addStatement("    // TODO: Show Error View")
                    .addStatement("}")
                    .addStatement("else -> {")
                    .addStatement("    // TODO: Show Content")
                    .addStatement("}")
                    .endControlFlow()
                    .addStatement("}")
                    .build(),
            )
            .build()
    }

    fun generateLayout(): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<layout xmlns:android="http://schemas.android.com/apk/res/android"""")
        appendLine("""    xmlns:app="http://schemas.android.com/apk/res-auto"""")
        appendLine("""    xmlns:tools="http://schemas.android.com/tools">""")
        appendLine()
        appendLine("""    <data>""")
        appendLine("""    </data>""")
        appendLine()
        appendLine("""    <androidx.constraintlayout.widget.ConstraintLayout""")
        appendLine("""        android:layout_width="match_parent"""")
        appendLine("""        android:layout_height="match_parent"""")
        appendLine("""        tools:context=".$screenPkg.${pascalName}Fragment">""")
        appendLine()
        appendLine("""        <!-- TODO: Add UI components -->""")
        appendLine()
        appendLine("""    </androidx.constraintlayout.widget.ConstraintLayout>""")
        appendLine()
        appendLine("""</layout>""")
    }
}
