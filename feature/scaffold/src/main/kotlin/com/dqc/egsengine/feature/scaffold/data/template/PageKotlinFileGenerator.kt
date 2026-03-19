package com.dqc.egsengine.feature.scaffold.data.template

import com.dqc.egsengine.feature.scaffold.domain.model.PageTemplate
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

/**
 * 页面 Kotlin 文件生成器 - 使用 KotlinPoet 生成 Contract/Fragment/ViewModel
 */
class PageKotlinFileGenerator(private val template: PageTemplate) {

    private val pascalName = template.pageName
    private val camelName = pascalName.replaceFirstChar { it.lowercase() }
    /** snake_case 布局名，如 task_detail */
    private val layoutSnakeName = pascalName.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    private val pkg = template.modulePackage
    private val fragmentPkg = "$pkg.presentation.fragment.$camelName"
    private val baseClasses = template.baseClassPackages
    private val basePackage = template.basePackage

    // 基础类名
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

    /**
     * 生成 Contract 文件
     * - State: data class (data, isLoading, error)，data 类型根据 useCases 可扩展
     * - Intent: sealed class，每个 useCase 对应一个请求意图（LoadData/Refresh + 其他）
     * - Effect: sealed class，仅 ShowToast
     */
    fun generateContract(): FileSpec {
        val intentClass = ClassName(fragmentPkg, "${pascalName}Contract", "Intent")
        val effectClass = ClassName(fragmentPkg, "${pascalName}Contract", "Effect")

        // State: data, isLoading, error + 每个 useCase 的返回值类型作为属性
        val modelPackage = "$pkg.domain.model"
        val stateBuilder = TypeSpec.classBuilder("State")
            .addSuperinterface(uiStateClass)
        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(ParameterSpec.builder("data", String::class).defaultValue("%S", "").build())
            .addParameter(ParameterSpec.builder("isLoading", Boolean::class).defaultValue("false").build())
            .addParameter(
                ParameterSpec.builder("error", String::class.asTypeName().copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
        val stateProperties = mutableListOf<PropertySpec>()
        stateProperties.add(PropertySpec.builder("data", String::class).initializer("data").build())
        stateProperties.add(PropertySpec.builder("isLoading", Boolean::class).initializer("isLoading").build())
        stateProperties.add(
            PropertySpec.builder("error", String::class.asTypeName().copy(nullable = true))
                .initializer("error")
                .build()
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
                        .build()
                )
                stateProperties.add(PropertySpec.builder(propName, typeClass.copy(nullable = true)).initializer(propName).build())
            }
        }

        val stateType = stateBuilder
            .primaryConstructor(constructorBuilder.build())
            .addProperties(stateProperties)
            .build()

        // Intent: sealed class，根据 useCases 动态生成 LoadData/Refresh 及每个 useCase 的 intent
        val intentNames = buildIntentNames()
        val intentType = TypeSpec.classBuilder("Intent")
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(uiIntentClass)
            .addTypes(
                intentNames.map { name ->
                    TypeSpec.objectBuilder(name)
                        .superclass(intentClass)
                        .build()
                }
            )
            .build()

        // Effect: sealed class，仅 ShowToast
        val effectType = TypeSpec.classBuilder("Effect")
            .addModifiers(KModifier.SEALED)
            .addSuperinterface(uiEffectClass)
            .addType(
                TypeSpec.classBuilder("ShowToast")
                    .superclass(effectClass)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("message", String::class)
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("message", String::class)
                            .initializer("message")
                            .build()
                    )
                    .build()
            )
            .build()

        return FileSpec.builder(fragmentPkg, "${pascalName}Contract")
            .addType(
                TypeSpec.interfaceBuilder("${pascalName}Contract")
                    .addType(stateType)
                    .addType(intentType)
                    .addType(effectType)
                    .build()
            )
            .build()
    }

    /**
     * 将包装类型映射为 Kotlin 标准类型，避免生成 ModelBoolean、KotlinBoolean 等。
     * 一般类型直接用 Kotlin 标准类型，自定义类型（如 SseEmitter）保持原样。
     */
    private fun resolveStatePropertyType(simpleType: String, typePackage: String, modelPackage: String): com.squareup.kotlinpoet.TypeName {
        return when (simpleType) {
            "Boolean", "ModelBoolean", "KotlinBoolean" -> Boolean::class.asTypeName()
            "Int", "ModelInt", "KotlinInt" -> Int::class.asTypeName()
            "Long", "ModelLong", "KotlinLong" -> Long::class.asTypeName()
            "String", "ModelString", "KotlinString" -> String::class.asTypeName()
            "Double", "ModelDouble", "KotlinDouble" -> Double::class.asTypeName()
            "Float", "ModelFloat", "KotlinFloat" -> Float::class.asTypeName()
            else -> ClassName(typePackage, simpleType)
        }
    }

    /** 根据 useCases 生成 Intent 名称：LoadData、Refresh + 每个 useCase 的意图 */
    private fun buildIntentNames(): List<String> {
        val base = listOf("LoadData", "Refresh")
        val fromUseCases = template.useCases.mapNotNull { useCase ->
            val name = useCase.name.removeSuffix("UseCase")
            if (name !in base) name else null
        }.distinct()
        return (base + fromUseCases)
    }

    /**
     * 生成 Fragment 文件
     */
    fun generateFragment(): FileSpec? {
        if (baseFragmentClass == null) return null

        val bindingClass = ClassName("$pkg.databinding", "Fragment${pascalName}Binding")
        val rClass = ClassName(pkg, "R")
        val stateClass = ClassName(fragmentPkg, "${pascalName}Contract.State")
        val effectClass = ClassName(fragmentPkg, "${pascalName}Contract.Effect")
        val viewModelClass = ClassName(fragmentPkg, "${pascalName}ViewModel")

        return FileSpec.builder(fragmentPkg, "${pascalName}Fragment")
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
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("onViewCreated")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("view", ClassName("android.view", "View"))
                            .addParameter("savedInstanceState", ClassName("android.os", "Bundle").copy(nullable = true))
                            .addStatement("super.onViewCreated(view, savedInstanceState)")
                            .addStatement("")
                            .addStatement("initObserver()")
                            .build()
                    )
                    .addFunction(generateInitObserverFun())
                    .addFunction(generateRenderStateFun(stateClass))
                    .addFunction(generateHandleEffectFun(effectClass))
                    .build()
            )
            .build()
    }

    private fun generateInitObserverFun(): FunSpec {
        return FunSpec.builder("initObserver")
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
    }

    private fun generateRenderStateFun(stateClass: ClassName): FunSpec {
        return FunSpec.builder("renderState")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("state", stateClass)
            .addStatement("binding.apply {")
            .addStatement("    // TODO: 绑定 UI")
            .addStatement("    // textView.text = state.data")
            .addStatement("    // progressBar.isVisible = state.isLoading")
            .addStatement("    // errorView.isVisible = state.error != null")
            .addStatement("}")
            .build()
    }

    private fun generateHandleEffectFun(effectClass: ClassName): FunSpec {
        return FunSpec.builder("handleEffect")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("effect", effectClass)
            .beginControlFlow("when (effect)")
            .addStatement("is ${pascalName}Contract.Effect.ShowToast -> showToast(effect.message)")
            .endControlFlow()
            .build()
    }

    /**
     * 生成 ViewModel 文件
     */
    fun generateViewModel(): FileSpec {
        val stateClass = ClassName(fragmentPkg, "${pascalName}Contract.State")
        val intentClass = ClassName(fragmentPkg, "${pascalName}Contract.Intent")
        val effectClass = ClassName(fragmentPkg, "${pascalName}Contract.Effect")

        val classBuilder = TypeSpec.classBuilder("${pascalName}ViewModel")
            .addModifiers(KModifier.INTERNAL)

        // 添加构造函数参数（UseCase 注入）
        if (template.useCases.isNotEmpty()) {
            val constructorBuilder = FunSpec.constructorBuilder()
            template.useCases.forEach { useCase ->
                val useCaseClass = ClassName(useCase.packageName, useCase.name)
                constructorBuilder.addParameter(useCase.camelName, useCaseClass)
            }
            classBuilder.primaryConstructor(constructorBuilder.build())

            // 添加 UseCase 属性
            template.useCases.forEach { useCase ->
                val useCaseClass = ClassName(useCase.packageName, useCase.name)
                classBuilder.addProperty(
                    PropertySpec.builder(useCase.camelName, useCaseClass)
                        .initializer(useCase.camelName)
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
            }
        }

        // 继承 BaseViewModel
        val baseVmWithTypes = if (baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            baseViewModelClass.parameterizedBy(stateClass, intentClass, effectClass)
        } else {
            baseViewModelClass
        }

        classBuilder.superclass(baseVmWithTypes)

        // 添加初始化 State
        if (baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            classBuilder.addSuperclassConstructorParameter(
                CodeBlock.of("%T()", stateClass)
            )
        }

        // 添加 onInitialize 方法
        classBuilder.addFunction(
            FunSpec.builder("onInitialize")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("dispatch(%T.LoadData)", intentClass)
                .build()
        )

        // 添加 registerIntents 方法
        val registerIntentsBuilder = FunSpec.builder("registerIntents")
            .addModifiers(KModifier.OVERRIDE)

        if (template.useCases.isNotEmpty() && baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            // 根据 Contract 的 Intent 动态添加 registerIntent
            buildIntentNames().forEach { intentName ->
                registerIntentsBuilder
                    .addStatement("registerIntent<%T.%L> {", intentClass, intentName)
                    .addStatement("    handleLoadData()")
                    .addStatement("}")
                    .addStatement("")
            }
        } else {
            registerIntentsBuilder.addStatement("// TODO: Register intent handlers")
        }

        classBuilder.addFunction(registerIntentsBuilder.build())

        // 如果有 UseCase，添加处理方法
        if (template.useCases.isNotEmpty()) {
            classBuilder.addFunction(generateHandleLoadDataFun(stateClass, effectClass))
        }

        return FileSpec.builder(fragmentPkg, "${pascalName}ViewModel")
            .addType(classBuilder.build())
            .build()
    }

    private fun generateHandleLoadDataFun(stateClass: ClassName, effectClass: ClassName): FunSpec {
        val funBuilder = FunSpec.builder("handleLoadData")
            .addModifiers(KModifier.PRIVATE)

        // 如果有 BaseViewModel 和 launchRequest 方法
        if (baseViewModelClass.canonicalName != "androidx.lifecycle.ViewModel") {
            funBuilder
                .beginControlFlow("launchRequest(")
                .addStatement("showLoading = true,")
                .addStatement("onError = { error ->")
                .addStatement("    updateState { copy(error = error.message) }")
                .addStatement("    sendEffect(%T.ShowToast(%S))", effectClass, "加载失败")
                .addStatement("}")
                .endControlFlow()
//                .addStatement(">")
                .addStatement("")

            // 添加 UseCase 调用
            template.useCases.firstOrNull()?.let { useCase ->
                funBuilder.addStatement("val result = ${useCase.camelName}()")
                funBuilder.addStatement("// TODO: 处理 result")
                funBuilder.addStatement("updateState { copy(isLoading = false) }")
            }

            funBuilder.addStatement("}")
        } else {
            funBuilder.addStatement("// TODO: 实现数据加载逻辑")
            template.useCases.firstOrNull()?.let { useCase ->
                funBuilder.addStatement("// val result = ${useCase.camelName}()")
            }
        }

        return funBuilder.build()
    }

    /**
     * 生成 Layout XML 文件
     */
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
        appendLine("""        tools:context=".$fragmentPkg.${pascalName}Fragment">""")
        appendLine()
        appendLine("""        <!-- TODO: Add UI components -->""")
        appendLine()
        appendLine("""    </androidx.constraintlayout.widget.ConstraintLayout>""")
        appendLine()
        appendLine("""</layout>""")
    }
}
