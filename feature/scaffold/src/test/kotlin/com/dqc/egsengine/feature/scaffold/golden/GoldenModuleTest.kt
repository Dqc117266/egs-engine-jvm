package com.dqc.egsengine.feature.scaffold.golden

import com.dqc.egsengine.feature.scaffold.ScaffoldTestFixtures
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory

/**
 * Full-tree golden snapshot of `create module` output for ANDROID and KMP projects.
 * Catches any unintended change in the wired `android/module` and `kmp/module` FTL templates.
 */
class GoldenModuleTest {

    @Test
    fun `android module golden`() {
        val files = ScaffoldTestFixtures.androidModuleGenerator().preview(tempRoot(), androidTemplate())
        GoldenSnapshot.verify("module-android", files.map { it.path to it.content })
    }

    @Test
    fun `kmp module golden`() {
        val files = ScaffoldTestFixtures.previewModule(tempRoot(), kmpTemplate())
        GoldenSnapshot.verify("module-kmp", files)
    }

    private fun tempRoot() = createTempDirectory("golden-module").toFile()

    private fun androidTemplate(): ModuleTemplate =
        ModuleTemplate(
            name = "task",
            packageName = "com.example.demo.feature.task",
            conventionPluginId = "com.example.demo.convention.feature",
            layers = listOf("data", "domain", "presentation"),
            hasRes = true,
            namespace = "com.example.demo.feature.task",
            projectType = "ANDROID",
            basePackage = "com.example.demo",
            baseClassPackages = BaseClassPackages(
                baseViewModel = "com.example.demo.feature.base.presentation.viewmodel.BaseViewModel",
                resultClass = "com.example.demo.feature.base.domain.result.Result",
            ),
        )

    private fun kmpTemplate(): ModuleTemplate =
        ModuleTemplate(
            name = "profile",
            packageName = "org.mifos.feature.profile",
            conventionPluginId = null,
            layers = listOf("data", "domain", "presentation"),
            hasRes = false,
            namespace = "org.mifos.feature.profile",
            projectType = "KMP",
            basePackage = "org.mifos",
            baseClassPackages = BaseClassPackages(
                baseViewModel = "template.core.base.ui.BaseViewModel",
                resultClass = "template.core.base.network.domain.Result",
            ),
        )
}
