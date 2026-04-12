/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.model.KmpModuleTemplateModel
import java.io.File

/**
 * Renders KMP feature module sources using FreeMarker under `templates/kmp/module/`.
 */
internal class KmpModuleTemplateRenderer(
    private val engine: TemplateEngine,
    private val model: KmpModuleTemplateModel,
) {
    private val sourceRoot = "src/commonMain/kotlin"
    private val m: Map<String, Any?> = modelToMap(model)

    private fun modelToMap(model: KmpModuleTemplateModel): Map<String, Any?> =
        mapOf(
            "pascal" to model.pascal,
            "camel" to model.camel,
            "packageName" to model.packageName,
            "basePackage" to model.basePackage,
            "namespace" to model.namespace,
            "pkgPath" to model.pkgPath,
            "conventionPluginAlias" to model.conventionPluginAlias,
            "presentationPkg" to model.presentationPkg,
        )

    fun renderBuildGradle(projectRoot: File? = null): String =
        engine.render("kmp/module/build.gradle.kts.ftl", m, projectRoot)

    fun renderRootKoinModule(projectRoot: File? = null): String =
        engine.render("kmp/module/RootKoinModule.kt.ftl", m, projectRoot)

    fun renderDataModule(projectRoot: File? = null): String =
        engine.render("kmp/module/DataModule.kt.ftl", m, projectRoot)

    fun renderDomainModule(projectRoot: File? = null): String =
        engine.render("kmp/module/DomainModule.kt.ftl", m, projectRoot)

    fun renderPresentationModule(projectRoot: File? = null): String =
        engine.render("kmp/module/PresentationModule.kt.ftl", m, projectRoot)

    fun renderRepository(projectRoot: File? = null): String =
        engine.render("kmp/module/Repository.kt.ftl", m, projectRoot)

    fun renderRepositoryImpl(projectRoot: File? = null): String =
        engine.render("kmp/module/RepositoryImpl.kt.ftl", m, projectRoot)

    fun renderContract(projectRoot: File? = null): String =
        engine.render("kmp/module/Contract.kt.ftl", m, projectRoot)

    fun renderViewModel(projectRoot: File? = null): String =
        engine.render("kmp/module/ViewModel.kt.ftl", m, projectRoot)

    fun renderScreen(projectRoot: File? = null): String =
        engine.render("kmp/module/Screen.kt.ftl", m, projectRoot)

    fun pathRootKoinModule(): String = "$sourceRoot/${model.pkgPath}/di/${model.pascal}Module.kt"

    fun pathDataModule(): String = "$sourceRoot/${model.pkgPath}/di/DataModule.kt"

    fun pathDomainModule(): String = "$sourceRoot/${model.pkgPath}/di/DomainModule.kt"

    fun pathPresentationModule(): String = "$sourceRoot/${model.pkgPath}/di/PresentationModule.kt"

    fun pathRepository(): String = "$sourceRoot/${model.pkgPath}/domain/repository/${model.pascal}Repository.kt"

    fun pathRepositoryImpl(): String = "$sourceRoot/${model.pkgPath}/data/repository/${model.pascal}RepositoryImpl.kt"

    fun pathContract(): String = "$sourceRoot/${model.pkgPath}/presentation/${model.camel}/${model.pascal}Contract.kt"

    fun pathViewModel(): String = "$sourceRoot/${model.pkgPath}/presentation/${model.camel}/${model.pascal}ViewModel.kt"

    fun pathScreen(): String = "$sourceRoot/${model.pkgPath}/presentation/${model.camel}/${model.pascal}Screen.kt"
}
