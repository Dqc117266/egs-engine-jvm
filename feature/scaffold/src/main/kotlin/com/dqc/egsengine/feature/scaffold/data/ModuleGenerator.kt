package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import com.dqc.egsengine.template.model.BaseClassPackagesModel
import com.dqc.egsengine.template.model.KmpModuleTemplateModel
import com.dqc.egsengine.template.model.ModuleTemplateModel
import org.slf4j.LoggerFactory
import java.io.File

class ModuleGenerator(
    private val templateEngine: TemplateEngine = TemplateEngine(TemplateRegistry()),
) {
    private val logger = LoggerFactory.getLogger(ModuleGenerator::class.java)

    data class GeneratedFile(val path: String, val content: String?)

    fun preview(projectRoot: File, template: ModuleTemplate): List<GeneratedFile> {
        return if (template.isKmpProject) {
            renderKmpModule(projectRoot, template)
        } else {
            renderAndroidModule(projectRoot, template)
        }
    }

    fun generate(projectRoot: File, template: ModuleTemplate): List<File> {
        val created = mutableListOf<File>()

        for (entry in preview(projectRoot, template)) {
            val file = projectRoot.resolve(entry.path)
            file.parentFile.mkdirs()

            if (entry.content != null) {
                file.writeText(entry.content)
            } else {
                file.createNewFile()
            }

            created.add(file)
            logger.debug("Created: ${entry.path}")
        }

        logger.info("Generated ${created.size} files for module '${template.name}'")
        return created
    }

    private fun renderAndroidModule(projectRoot: File, template: ModuleTemplate): List<GeneratedFile> {
        val pascal = template.name.toPascalCase()
        val pkgPath = template.packageName.toPath()
        val moduleDir = "feature/${template.name}"
        val model = template.toAndroidModel(pascal)

        return listOf(
            render(projectRoot, "android/module/build.gradle.kts.ftl", model, "$moduleDir/build.gradle.kts"),
            render(
                projectRoot,
                "android/module/KoinModule.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/${pascal}KoinModule.kt",
            ),
            render(
                projectRoot,
                "android/module/DataModule.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/data/DataModule.kt",
            ),
            render(
                projectRoot,
                "android/module/DomainModule.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/domain/DomainModule.kt",
            ),
            render(
                projectRoot,
                "android/module/PresentationModule.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/presentation/PresentationModule.kt",
            ),
            render(
                projectRoot,
                "android/module/Repository.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/domain/repository/${pascal}Repository.kt",
            ),
            render(
                projectRoot,
                "android/module/RepositoryImpl.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/data/repository/${pascal}RepositoryImpl.kt",
            ),
            render(
                projectRoot,
                "android/module/ViewModel.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/presentation/screen/${pascal}ViewModel.kt",
            ),
            render(
                projectRoot,
                "android/module/Contract.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/presentation/screen/${pascal}Contract.kt",
            ),
            render(
                projectRoot,
                "android/module/NavigationRoute.kt.ftl",
                model,
                "$moduleDir/src/main/kotlin/$pkgPath/presentation/${pascal}NavigationRoute.kt",
            ),
            render(
                projectRoot,
                "android/module/AndroidManifest.xml.ftl",
                model,
                "$moduleDir/src/main/AndroidManifest.xml",
            ),
        )
    }

    private fun renderKmpModule(projectRoot: File, template: ModuleTemplate): List<GeneratedFile> {
        val pascal = template.name.toPascalCase()
        val camel = pascal.replaceFirstChar { it.lowercase() }
        val pkgPath = template.packageName.toPath()
        val moduleDir = "feature/${template.name}"
        val sourceRoot = "$moduleDir/src/commonMain/kotlin/$pkgPath"
        val model = template.toKmpModel(pascal, camel, pkgPath)

        return listOf(
            render(projectRoot, "kmp/module/build.gradle.kts.ftl", model, "$moduleDir/build.gradle.kts"),
            render(
                projectRoot,
                "kmp/module/RootKoinModule.kt.ftl",
                model,
                "$sourceRoot/di/${pascal}KoinModule.kt",
            ),
            render(projectRoot, "kmp/module/DataModule.kt.ftl", model, "$sourceRoot/di/DataModule.kt"),
            render(projectRoot, "kmp/module/DomainModule.kt.ftl", model, "$sourceRoot/di/DomainModule.kt"),
            render(
                projectRoot,
                "kmp/module/PresentationModule.kt.ftl",
                model,
                "$sourceRoot/di/PresentationModule.kt",
            ),
            render(
                projectRoot,
                "kmp/module/Repository.kt.ftl",
                model,
                "$sourceRoot/domain/repository/${pascal}Repository.kt",
            ),
            render(
                projectRoot,
                "kmp/module/RepositoryImpl.kt.ftl",
                model,
                "$sourceRoot/data/repository/${pascal}RepositoryImpl.kt",
            ),
            render(projectRoot, "kmp/module/Contract.kt.ftl", model, "$sourceRoot/presentation/$camel/${pascal}Contract.kt"),
            render(projectRoot, "kmp/module/ViewModel.kt.ftl", model, "$sourceRoot/presentation/$camel/${pascal}ViewModel.kt"),
            render(projectRoot, "kmp/module/Screen.kt.ftl", model, "$sourceRoot/presentation/$camel/${pascal}Screen.kt"),
        )
    }

    private fun render(projectRoot: File, templateName: String, model: Any, path: String): GeneratedFile =
        GeneratedFile(
            path = path,
            content = templateEngine.render(templateName, model, projectRoot),
        )

    private fun ModuleTemplate.toAndroidModel(pascal: String): ModuleTemplateModel {
        val camel = pascal.replaceFirstChar { it.lowercase() }
        val baseViewModelPackage = baseClassPackages.baseViewModel?.substringBeforeLast(".")
        val uiContractPackage = baseViewModelPackage
            ?: basePackage?.let { "$it.feature.base.presentation.viewmodel" }
            ?: "com.example.feature.base.presentation.viewmodel"
        val resultClass = baseClassPackages.resultClass
        val resultSimpleName = resultClass?.substringAfterLast(".") ?: "Result"
        val repositoryImports = resultClass.asImportList()
        val repositoryImplImports = buildList {
            add("$packageName.domain.repository.${pascal}Repository")
            resultClass?.let { add(it) }
            baseClassPackages.retrofitProvider?.let { add(it) }
        }
        val retrofitProviderSimpleName = baseClassPackages.retrofitProvider?.substringAfterLast(".")
        val baseViewModelSimpleName = baseClassPackages.baseViewModel?.substringAfterLast(".")

        return ModuleTemplateModel(
            pascal = pascal,
            camel = camel,
            layoutSnakeName = pascal.toSnakeCase(),
            packageName = packageName,
            android = true,
            basePackage = basePackage,
            baseClasses = baseClassPackages.toModel(),
            uiContractPackage = uiContractPackage,
            conventionPluginId = conventionPluginId,
            namespace = namespace,
            repositoryReturnType = "$resultSimpleName<String>",
            repositoryImports = repositoryImports,
            repositoryImplImports = repositoryImplImports,
            hasRetrofit = baseClassPackages.retrofitProvider != null,
            retrofitProviderFqcn = baseClassPackages.retrofitProvider,
            retrofitProviderSimpleName = retrofitProviderSimpleName,
            hasBaseViewModel = baseClassPackages.baseViewModel != null,
            baseViewModelImport = baseClassPackages.baseViewModel,
            baseViewModelSimpleName = baseViewModelSimpleName,
        )
    }

    private fun ModuleTemplate.toKmpModel(pascal: String, camel: String, pkgPath: String): KmpModuleTemplateModel {
        val resolvedBasePackage = basePackage ?: packageName.substringBefore(".feature.", packageName)
        val resolvedNamespace = namespace ?: packageName
        return KmpModuleTemplateModel(
            pascal = pascal,
            camel = camel,
            packageName = packageName,
            basePackage = resolvedBasePackage,
            namespace = resolvedNamespace,
            pkgPath = pkgPath,
            conventionPluginAlias = conventionPluginId.toVersionCatalogAlias(),
            presentationPkg = "$packageName.presentation.$camel",
        )
    }

    private fun com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages.toModel(): BaseClassPackagesModel =
        BaseClassPackagesModel(
            baseViewModel = baseViewModel,
            baseFragment = baseFragment,
            resultClass = resultClass,
            retrofitProvider = retrofitProvider,
        )

    private val ModuleTemplate.isKmpProject: Boolean
        get() = projectType in setOf("KMP", "KMP_ANDROID")

    private fun String?.asImportList(): List<String> = this?.let { listOf(it) }.orEmpty()

    private fun String?.toVersionCatalogAlias(): String =
        this?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.removePrefix("libs.plugins.")
            ?.replace('-', '.')
            ?: "cmp.feature.convention"

    private fun String.toPath(): String = replace('.', '/')

    private fun String.toPascalCase(): String =
        split('-', '_').joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }

    private fun String.toSnakeCase(): String =
        replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
}
