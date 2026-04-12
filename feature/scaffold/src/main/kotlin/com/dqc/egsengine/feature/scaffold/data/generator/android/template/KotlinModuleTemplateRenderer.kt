/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.android.template

import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.model.ModuleTemplateModel
import java.io.File

/**
 * Renders feature module skeleton Kotlin/XML via FreeMarker templates.
 */
class KotlinModuleTemplateRenderer(
    private val engine: TemplateEngine,
    private val template: ModuleTemplate,
) {
    private val m: ModuleTemplateModel = template.toModuleTemplateModel()
    private val pkg: String get() = m.packageName
    private val pascal: String get() = m.pascal

    fun renderRootKoinModule(projectRoot: File? = null): String =
        engine.render("android/module/KoinModule.kt.ftl", m, projectRoot)

    fun renderDataModule(projectRoot: File? = null): String =
        engine.render("android/module/DataModule.kt.ftl", m, projectRoot)

    fun renderDomainModule(projectRoot: File? = null): String =
        engine.render("android/module/DomainModule.kt.ftl", m, projectRoot)

    fun renderPresentationModule(projectRoot: File? = null): String =
        engine.render("android/module/PresentationModule.kt.ftl", m, projectRoot)

    fun renderRepositoryInterface(projectRoot: File? = null): String =
        engine.render("android/module/Repository.kt.ftl", m, projectRoot)

    fun renderRepositoryImpl(projectRoot: File? = null): String =
        engine.render("android/module/RepositoryImpl.kt.ftl", m, projectRoot)

    fun renderViewModel(projectRoot: File? = null): String =
        engine.render("android/module/ViewModel.kt.ftl", m, projectRoot)

    fun renderContract(projectRoot: File? = null): String? =
        if (!m.android) null else engine.render("android/module/Contract.kt.ftl", m, projectRoot)

    fun renderNavigationRoute(projectRoot: File? = null): String? =
        if (!m.android) null else engine.render("android/module/NavigationRoute.kt.ftl", m, projectRoot)

    fun renderBuildGradle(projectRoot: File? = null): String =
        engine.render("android/module/build.gradle.kts.ftl", m, projectRoot)

    fun renderAndroidManifest(projectRoot: File? = null): String =
        engine.render("android/module/AndroidManifest.xml.ftl", m, projectRoot)

    fun kotlinRelativePath(packageName: String, simpleFileName: String): String =
        "src/main/kotlin/${packageName.replace('.', '/')}/$simpleFileName.kt"

    fun pathRootKoinModule(): String = kotlinRelativePath(pkg, "${pascal}KoinModule")
    fun pathDataModule(): String = kotlinRelativePath("$pkg.data", "DataModule")
    fun pathDomainModule(): String = kotlinRelativePath("$pkg.domain", "DomainModule")
    fun pathPresentationModule(): String = kotlinRelativePath("$pkg.presentation", "PresentationModule")
    fun pathRepository(): String = kotlinRelativePath("$pkg.domain.repository", "${pascal}Repository")
    fun pathRepositoryImpl(): String = kotlinRelativePath("$pkg.data.repository", "${pascal}RepositoryImpl")
    fun pathViewModel(): String = kotlinRelativePath("$pkg.presentation.screen", "${pascal}ViewModel")
    fun pathContract(): String = kotlinRelativePath("$pkg.presentation.screen", "${pascal}Contract")
    fun pathNavigationRoute(): String = kotlinRelativePath("$pkg.presentation", "${pascal}NavigationRoute")
}
