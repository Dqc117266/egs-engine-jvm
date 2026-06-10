package com.dqc.egsengine.feature.scaffold.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureDiUpdaterKmpTest {
    @Test
    fun `updatePresentationModule uses presentation camel package for KMP layout`() {
        val root =
            kotlin.io.path
                .createTempDirectory("feature-di-kmp")
                .toFile()
        val pkg = "com.example.feature.task"
        val pkgPath = pkg.replace(".", "/")
        val pm = root.resolve("feature/task/src/commonMain/kotlin/$pkgPath/presentation/PresentationModule.kt")
        pm.parentFile.mkdirs()
        pm.writeText(
            """
            package com.example.feature.task.presentation

            import org.koin.dsl.module

            internal val presentationModule = module {
                // ViewModels will be registered here
            }
            """.trimIndent(),
        )

        val updater = FeatureDiUpdater()
        updater.updatePresentationModule(
            projectRoot = root,
            moduleName = "task",
            modulePackage = pkg,
            pageName = "TodoDetail",
            useCases = emptyList(),
            kotlinRootRel = "src/commonMain/kotlin",
            useScreenPresentationLayout = true,
        )

        val text = pm.readText()
        assertTrue(text.contains("import com.example.feature.task.presentation.screen.todoDetail.TodoDetailViewModel"))
    }

    @Test
    fun `updatePresentationModule uses presentation fragment package when useScreenPresentationLayout false`() {
        val root =
            kotlin.io.path
                .createTempDirectory("feature-di-frag")
                .toFile()
        val pkg = "com.example.feature.legacy"
        val pkgPath = pkg.replace(".", "/")
        val pm = root.resolve("feature/legacy/src/main/kotlin/$pkgPath/presentation/PresentationModule.kt")
        pm.parentFile.mkdirs()
        pm.writeText(
            """
            package com.example.feature.legacy.presentation

            import org.koin.dsl.module

            internal val presentationModule = module {
            }
            """.trimIndent(),
        )

        val updater = FeatureDiUpdater()
        updater.updatePresentationModule(
            projectRoot = root,
            moduleName = "legacy",
            modulePackage = pkg,
            pageName = "OldPage",
            useCases = emptyList(),
            kotlinRootRel = "src/main/kotlin",
            useScreenPresentationLayout = false,
        )

        val text = pm.readText()
        assertTrue(text.contains("import com.example.feature.legacy.presentation.fragment.oldPage.OldPageViewModel"))
    }

    @Test
    fun `updatePresentationModule is idempotent when ViewModel already registered`() {
        val root =
            kotlin.io.path
                .createTempDirectory("feature-di-kmp-idem")
                .toFile()
        val pkg = "com.example.feature.task"
        val pkgPath = pkg.replace(".", "/")
        val pm = root.resolve("feature/task/src/commonMain/kotlin/$pkgPath/presentation/PresentationModule.kt")
        pm.parentFile.mkdirs()
        pm.writeText(
            """
            package com.example.feature.task.presentation

            import com.example.feature.task.presentation.screen.todoDetail.TodoDetailViewModel
            import org.koin.core.module.dsl.viewModelOf
            import org.koin.dsl.module

            internal val presentationModule = module {
                viewModelOf(::TodoDetailViewModel)
            }
            """.trimIndent(),
        )

        val updater = FeatureDiUpdater()
        val second =
            updater.updatePresentationModule(
                projectRoot = root,
                moduleName = "task",
                modulePackage = pkg,
                pageName = "TodoDetail",
                useCases = emptyList(),
                kotlinRootRel = "src/commonMain/kotlin",
                useScreenPresentationLayout = true,
            )

        assertFalse(second)
        assertEquals(1, pm.readText().lines().count { it.contains("viewModelOf(::TodoDetailViewModel)") })
    }
}
