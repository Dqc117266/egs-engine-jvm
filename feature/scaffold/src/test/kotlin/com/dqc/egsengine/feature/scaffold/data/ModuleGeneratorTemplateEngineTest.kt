package com.dqc.egsengine.feature.scaffold.data

import com.dqc.egsengine.feature.scaffold.ScaffoldTestFixtures
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModuleGeneratorTemplateEngineTest {
    @Test
    fun `preview uses project local android module template overrides`() {
        val projectRoot =
            kotlin.io.path
                .createTempDirectory("module-ftl-override")
                .toFile()
        val override = projectRoot.resolve(".egs/templates/android/module/ViewModel.kt.ftl")
        override.parentFile.mkdirs()
        override.writeText(
            """
            package ${'$'}{packageName}.presentation.screen

            internal class ${'$'}{pascal}ViewModel {
                fun marker(): String = "project override"
            }
            """.trimIndent(),
        )

        val files = ScaffoldTestFixtures.androidModuleGenerator().preview(projectRoot, androidTemplate())

        val viewModel = files.single { it.path.endsWith("TaskViewModel.kt") }
        assertNotNull(viewModel.content)
        assertTrue(viewModel.content!!.contains("project override"))
        assertEquals(
            "feature/task/src/main/kotlin/com/example/demo/feature/task/presentation/screen/TaskViewModel.kt",
            viewModel.path,
        )
    }

    @Test
    fun `preview renders kmp module templates into commonMain layout`() {
        val projectRoot =
            kotlin.io.path
                .createTempDirectory("module-ftl-kmp")
                .toFile()

        val files = ScaffoldTestFixtures.previewModule(projectRoot, kmpTemplate())

        val viewModel = files.single { it.first.endsWith("ProfileViewModel.kt") }
        assertNotNull(viewModel.second)
        assertTrue(viewModel.second!!.contains("Feature module (KMP)"))
        assertEquals(
            "feature/profile/src/commonMain/kotlin/org/mifos/feature/profile/presentation/profile/ProfileViewModel.kt",
            viewModel.first,
        )
        assertTrue(files.any { it.first.endsWith("ProfileScreen.kt") })
        assertTrue(files.none { it.first.contains("/src/main/kotlin/") })
    }

    private fun androidTemplate(): ModuleTemplate = ModuleTemplate(
        name = "task",
        packageName = "com.example.demo.feature.task",
        conventionPluginId = "com.example.demo.convention.feature",
        layers = listOf("data", "domain", "presentation"),
        hasRes = true,
        namespace = "com.example.demo.feature.task",
        projectType = "ANDROID",
        basePackage = "com.example.demo",
        baseClassPackages =
        BaseClassPackages(
            baseViewModel = "com.example.demo.feature.base.presentation.viewmodel.BaseViewModel",
            resultClass = "com.example.demo.feature.base.domain.result.Result",
        ),
    )

    private fun kmpTemplate(): ModuleTemplate = ModuleTemplate(
        name = "profile",
        packageName = "org.mifos.feature.profile",
        conventionPluginId = null,
        layers = listOf("data", "domain", "presentation"),
        hasRes = false,
        namespace = "org.mifos.feature.profile",
        projectType = "KMP",
        basePackage = "org.mifos",
        baseClassPackages =
        BaseClassPackages(
            baseViewModel = "template.core.base.ui.BaseViewModel",
            resultClass = "template.core.base.network.domain.Result",
        ),
    )
}
