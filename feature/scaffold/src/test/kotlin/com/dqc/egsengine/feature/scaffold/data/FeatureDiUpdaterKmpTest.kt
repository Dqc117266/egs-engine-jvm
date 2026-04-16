package com.dqc.egsengine.feature.scaffold.data

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FeatureDiUpdaterKmpTest {

    @Test
    fun `updatePresentationModule uses presentation camel package for KMP layout`() {
        val root = kotlin.io.path.createTempDirectory("feature-di-kmp").toFile()
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
            useKmpPresentationLayout = true,
        )

        val text = pm.readText()
        assertTrue(text.contains("import com.example.feature.task.presentation.screen.todoDetail.TodoDetailViewModel"))
    }
}
