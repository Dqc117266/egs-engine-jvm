package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class KmpGeneratedModuleWireUpdaterTest {

    @Test
    fun `wire is idempotent`() {
        val root = kotlin.io.path.createTempDirectory("kmp-wire").toFile()
        val moduleName = "todolist"
        val pkg = "org.mifos.feature.todolist"
        val pkgPath = pkg.replace('.', '/')
        val kt = root.resolve(
            "feature/$moduleName/src/commonMain/kotlin/$pkgPath/TodolistModule.kt",
        )
        kt.parentFile.mkdirs()
        kt.writeText(
            """
            package $pkg

            import org.koin.core.module.Module
            import org.koin.dsl.module
            import $pkg.data.dataModule
            import $pkg.domain.domainModule
            import $pkg.presentation.presentationModule

            val featureTodolistModules: List<Module> = listOf(
                dataModule,
                domainModule,
                presentationModule,
            )

            val TodolistModule: Module = module {
                includes(featureTodolistModules)
            }
            """.trimIndent(),
        )

        val config = SubProjectConfig(
            platform = Platform.KMP,
            path = ".",
            basePackage = "org.mifos",
            conventionPluginId = "org.convention.cmp.feature",
        )

        val updater = KmpGeneratedModuleWireUpdater()
        updater.wireIfNeeded(root, moduleName, config)
        val once = kt.readText()
        updater.wireIfNeeded(root, moduleName, config)
        val twice = kt.readText()

        assertEquals(once, twice)
        assertTrue(once.contains("generatedDataModule,"))
        assertTrue(once.contains("generatedDomainModule,"))
        assertTrue(once.contains("import $pkg.generate.generatedDataModule"))
    }
}
