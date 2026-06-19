package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.scaffold.data.EgsConfigReader
import com.dqc.egsengine.feature.scaffold.data.FeatureDiUpdater
import com.dqc.egsengine.feature.scaffold.data.UseCaseScanner
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseInfo
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ViewModelEditScaffolderTest {
    @field:TempDir
    lateinit var projectRoot: File

    @Test
    fun `append second use case then idempotent second run`() {
        val clientRoot = createKmpClientRoot(projectRoot)
        val moduleName = "wmtest"
        writeUseCaseFiles(clientRoot, moduleName)

        val pageScaffolder =
            PageScaffolder(
                configReader = EgsConfigReader(),
                useCaseScanner = UseCaseScanner(),
                diUpdater = FeatureDiUpdater(),
                templateEngine = TemplateEngine(TemplateRegistry()),
                clientAppNavigationWiring =
                com.dqc.egsengine.feature.scaffold.data
                    .ClientAppNavigationWiring(),
            )

        val first =
            UseCaseInfo(
                name = "FirstPingUseCase",
                packageName = "com.dqc.example.feature.wmtest.domain.usecase",
                path =
                "feature/wmtest/src/commonMain/kotlin/com/dqc/example/feature/wmtest/domain/usecase/FirstPingUseCase.kt",
                returnType = "template.core.base.network.domain.Result<String>",
            )

        pageScaffolder.scaffold(
            projectRoot = clientRoot,
            moduleName = moduleName,
            pageName = "VmMerge",
            useCases = listOf(first),
            dryRun = false,
            workspaceRoot = clientRoot,
            pagingOption = "none",
            skipNav = true,
            skipAppWire = true,
        )

        val vmPath =
            clientRoot.resolve(
                "feature/wmtest/src/commonMain/kotlin/com/dqc/example/feature/wmtest/presentation/screen/vmMerge/VmMergeViewModel.kt",
            )
        assertTrue(vmPath.isFile, "expected scaffold to create ViewModel")
        val vmBefore = vmPath.readText()
        assertTrue(vmBefore.contains("FirstPingUseCase"), "expected ctor wiring")

        val scanner = UseCaseScanner()
        val second =
            scanner.scanByModule(clientRoot, moduleName).single { it.name == "SecondPongUseCase" }

        val editScaffolder =
            ViewModelEditScaffolder(
                configReader = EgsConfigReader(),
                useCaseScanner = scanner,
                featureDiUpdater = FeatureDiUpdater(),
            )

        val r1 =
            editScaffolder.edit(
                projectRoot = clientRoot,
                moduleName = moduleName,
                pageName = "VmMerge",
                selectedUseCases = listOf(second),
                dryRun = false,
                workspaceRoot = clientRoot,
                pagingOption = "none",
            )
        assertTrue(r1.pageScaffoldResult.files.isNotEmpty(), "first edit should modify files")
        val vmAfter = vmPath.readText()
        assertTrue(vmAfter.contains("SecondPongUseCase"), "expected second ctor")
        assertTrue(vmAfter.contains("handleSecondPong"), "expected handler")

        val r2 =
            editScaffolder.edit(
                projectRoot = clientRoot,
                moduleName = moduleName,
                pageName = "VmMerge",
                selectedUseCases = listOf(second),
                dryRun = false,
                workspaceRoot = clientRoot,
                pagingOption = "none",
            )
        assertEquals(0, r2.pageScaffoldResult.files.size, "second run should be idempotent")
    }

    @Test
    fun `preserves trailing companion object`() {
        val clientRoot = createKmpClientRoot(projectRoot)
        val moduleName = "wmtest2"
        writeUseCaseFiles(clientRoot, moduleName)

        val pageScaffolder =
            PageScaffolder(
                configReader = EgsConfigReader(),
                useCaseScanner = UseCaseScanner(),
                diUpdater = FeatureDiUpdater(),
                templateEngine = TemplateEngine(TemplateRegistry()),
                clientAppNavigationWiring =
                com.dqc.egsengine.feature.scaffold.data
                    .ClientAppNavigationWiring(),
            )

        val first =
            UseCaseInfo(
                name = "FirstPingUseCase",
                packageName = "com.dqc.example.feature.wmtest2.domain.usecase",
                path =
                "feature/wmtest2/src/commonMain/kotlin/com/dqc/example/feature/wmtest2/domain/usecase/FirstPingUseCase.kt",
                returnType = "template.core.base.network.domain.Result<String>",
            )

        pageScaffolder.scaffold(
            projectRoot = clientRoot,
            moduleName = moduleName,
            pageName = "Companion",
            useCases = listOf(first),
            dryRun = false,
            workspaceRoot = clientRoot,
            pagingOption = "none",
            skipNav = true,
            skipAppWire = true,
        )

        val vmPath =
            clientRoot.resolve(
                "feature/wmtest2/src/commonMain/kotlin/com/dqc/example/feature/wmtest2/presentation/screen/companion/CompanionViewModel.kt",
            )
        val original = vmPath.readText()
        val withCompanion =
            original
                .trimEnd()
                .removeSuffix("}")
                .trimEnd() +
                "\n\n    companion object {\n        const val X = 1\n    }\n}\n"
        vmPath.writeText(withCompanion)

        val second =
            UseCaseScanner().scanByModule(clientRoot, moduleName).single { it.name == "SecondPongUseCase" }

        val r =
            ViewModelEditScaffolder(
                configReader = EgsConfigReader(),
                useCaseScanner = UseCaseScanner(),
                featureDiUpdater = FeatureDiUpdater(),
            ).edit(
                projectRoot = clientRoot,
                moduleName = moduleName,
                pageName = "Companion",
                selectedUseCases = listOf(second),
                dryRun = false,
                workspaceRoot = clientRoot,
                pagingOption = "none",
            )
        assertTrue(r.pageScaffoldResult.files.isNotEmpty())
        val text = vmPath.readText()
        assertTrue(text.contains("companion object"), "companion should remain")
        assertTrue(text.contains("handleSecondPong"), "new handler present")
        assertTrue(text.indexOf("handleSecondPong") < text.indexOf("companion object"), "handler before companion")
    }

    private fun createKmpClientRoot(root: File): File {
        root.resolve(".egs").mkdirs()
        root.resolve(".egs/config.json").writeText(
            """
            {
              "projectName": "fixture-kmp",
              "projectType": "KMP",
              "rootPath": "${root.absolutePath.replace("\\", "\\\\")}",
              "conventionPluginId": "com.dqc.example.convention.feature",
              "basePackage": "com.dqc.example",
              "moduleStructure": {
                "layers": ["data", "domain", "presentation"],
                "hasRes": false
              },
              "baseClasses": []
            }
            """.trimIndent(),
        )
        root.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "fixture-kmp"
            """.trimIndent(),
        )
        return root
    }

    private fun writeUseCaseFiles(
        clientRoot: File,
        moduleName: String,
    ) {
        val pkg = "com.dqc.example.feature.$moduleName.domain.usecase"
        val base = "feature/$moduleName/src/commonMain/kotlin/${pkg.replace(".", "/")}"
        val rt = "template.core.base.network.domain.Result<String>"

        fun write(name: String) {
            val f = clientRoot.resolve("$base/$name.kt")
            f.parentFile.mkdirs()
            f.writeText(
                """
                package $pkg

                class $name {
                    suspend operator fun invoke(): $rt {
                        throw NotImplementedError()
                    }
                }
                """.trimIndent(),
            )
        }
        write("FirstPingUseCase")
        write("SecondPongUseCase")
    }
}
