package com.dqc.egsengine.feature.scaffold.data.template

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

class KotlinFileGenerator(private val template: ModuleTemplate) {

    private val pascal = template.name.toPascalCase()
    private val camel = pascal.replaceFirstChar { it.lowercase() }
    private val pkg = template.packageName
    private val isAndroid = template.projectType in listOf("ANDROID", "KMP_ANDROID")
    private val bcp = template.baseClassPackages

    private val moduleClass = ClassName("org.koin.core.module", "Module")
    private val koinModule = MemberName("org.koin.dsl", "module")
    private val singleOf = MemberName("org.koin.core.module.dsl", "singleOf")
    private val bind = MemberName("org.koin.core.module.dsl", "bind")
    private val viewModelOf = MemberName("org.koin.core.module.dsl", "viewModelOf")

    fun generateRootKoinModule(): FileSpec {
        val dataModuleName = ClassName("$pkg.data", "dataModule")
        val domainModuleName = ClassName("$pkg.domain", "domainModule")
        val presentationModuleName = ClassName("$pkg.presentation", "presentationModule")

        return FileSpec.builder(pkg, "${pascal}KoinModule")
            .addProperty(
                PropertySpec.builder(
                    "feature${pascal}Modules",
                    ClassName("kotlin.collections", "List").parameterizedBy(moduleClass),
                )
                    .initializer(
                        CodeBlock.of(
                            "listOf(\n    %T,\n    %T,\n    %T,\n)",
                            presentationModuleName,
                            domainModuleName,
                            dataModuleName,
                        ),
                    )
                    .build(),
            )
            .build()
    }

    fun generateDataModule(): FileSpec {
        val repoImplClass = ClassName("$pkg.data.repository", "${pascal}RepositoryImpl")
        val repoInterface = ClassName("$pkg.domain.repository", "${pascal}Repository")

        return FileSpec.builder("$pkg.data", "DataModule")
            .addProperty(
                PropertySpec.builder("dataModule", moduleClass)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer(
                        CodeBlock.builder()
                            .beginControlFlow("%M", koinModule)
                            .addStatement("")
                            .addStatement(
                                "%M(::%T) { %M<%T>() }",
                                singleOf, repoImplClass, bind, repoInterface,
                            )
                            .endControlFlow()
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    fun generateDomainModule(): FileSpec =
        FileSpec.builder("$pkg.domain", "DomainModule")
            .addProperty(
                PropertySpec.builder("domainModule", moduleClass)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer(
                        CodeBlock.builder()
                            .beginControlFlow("%M", koinModule)
                            .addStatement("// register domain dependencies here")
                            .endControlFlow()
                            .build(),
                    )
                    .build(),
            )
            .build()

    fun generatePresentationModule(): FileSpec {
        val viewModelClass = ClassName("$pkg.presentation.viewmodel", "${pascal}ViewModel")

        return FileSpec.builder("$pkg.presentation", "PresentationModule")
            .addProperty(
                PropertySpec.builder("presentationModule", moduleClass)
                    .addModifiers(KModifier.INTERNAL)
                    .initializer(
                        CodeBlock.builder()
                            .beginControlFlow("%M", koinModule)
                            .addStatement("%M(::%T)", viewModelOf, viewModelClass)
                            .endControlFlow()
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    fun generateRepositoryInterface(): FileSpec {
        val resultType = resolveResultType()

        val builder = TypeSpec.interfaceBuilder("${pascal}Repository")
            .addModifiers(KModifier.INTERNAL)
            .addFunction(
                FunSpec.builder("getData")
                    .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                    .returns(resultType.parameterizedBy(ClassName("kotlin", "String")))
                    .build(),
            )

        return FileSpec.builder("$pkg.domain.repository", "${pascal}Repository")
            .addType(builder.build())
            .build()
    }

    fun generateRepositoryImpl(): FileSpec {
        val repoInterface = ClassName("$pkg.domain.repository", "${pascal}Repository")
        val resultType = resolveResultType()

        val classBuilder = TypeSpec.classBuilder("${pascal}RepositoryImpl")
            .addModifiers(KModifier.INTERNAL)
            .addSuperinterface(repoInterface)

        if (isAndroid && bcp.retrofitProvider != null) {
            val retrofitProvider = bcp.retrofitProvider.toClassName()
            classBuilder.primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("retrofitProvider", retrofitProvider)
                    .build(),
            )
            classBuilder.addProperty(
                PropertySpec.builder("retrofitProvider", retrofitProvider)
                    .initializer("retrofitProvider")
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )
        }

        classBuilder.addFunction(
            FunSpec.builder("getData")
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .returns(resultType.parameterizedBy(ClassName("kotlin", "String")))
                .addStatement("TODO(\"Not yet implemented\")")
                .build(),
        )

        return FileSpec.builder("$pkg.data.repository", "${pascal}RepositoryImpl")
            .addType(classBuilder.build())
            .build()
    }

    fun generateViewModel(): FileSpec {
        val repoInterface = ClassName("$pkg.domain.repository", "${pascal}Repository")

        val classBuilder = TypeSpec.classBuilder("${pascal}ViewModel")
            .addModifiers(KModifier.INTERNAL)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("repository", repoInterface)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("repository", repoInterface)
                    .initializer("repository")
                    .addModifiers(KModifier.PRIVATE)
                    .build(),
            )

        if (isAndroid && bcp.baseViewModel != null) {
            classBuilder.superclass(bcp.baseViewModel.toClassName())
        } else {
            classBuilder.superclass(ClassName("androidx.lifecycle", "ViewModel"))
        }

        return FileSpec.builder("$pkg.presentation.viewmodel", "${pascal}ViewModel")
            .addType(classBuilder.build())
            .build()
    }

    fun generateFragment(): FileSpec? {
        if (!isAndroid) return null
        if (bcp.baseFragment == null) return null

        val baseFragment = bcp.baseFragment.toClassName()
        val bindingClass = ClassName("$pkg.databinding", "Fragment${pascal}Binding")
        val rClass = ClassName(pkg, "R")
        val bundleClass = ClassName("android.os", "Bundle")
        val viewClass = ClassName("android.view", "View")

        return FileSpec.builder("$pkg.presentation.fragment.$camel", "${pascal}Fragment")
            .addType(
                TypeSpec.classBuilder("${pascal}Fragment")
                    .superclass(baseFragment.parameterizedBy(bindingClass))
                    .addSuperclassConstructorParameter("%T.layout.fragment_$camel", rClass)
                    .addFunction(
                        FunSpec.builder("onViewCreated")
                            .addModifiers(KModifier.OVERRIDE)
                            .addParameter("view", viewClass)
                            .addParameter("savedInstanceState", bundleClass.copy(nullable = true))
                            .addStatement("super.onViewCreated(view, savedInstanceState)")
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    private fun resolveResultType(): ClassName =
        if (bcp.resultClass != null) {
            bcp.resultClass.toClassName()
        } else {
            ClassName("kotlin", "Result")
        }

    private fun String.toClassName(): ClassName {
        val parts = split(".")
        val packageName = parts.dropLast(1).joinToString(".")
        val simpleName = parts.last()
        return ClassName(packageName, simpleName)
    }

    private fun String.toPascalCase(): String =
        split("-", "_").joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
}

internal fun FileSpec.toFixedString(): String =
    toString().replace("`data`", "data")
