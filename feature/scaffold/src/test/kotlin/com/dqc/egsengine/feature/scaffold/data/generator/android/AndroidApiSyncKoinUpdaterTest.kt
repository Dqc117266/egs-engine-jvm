package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class AndroidApiSyncKoinUpdaterTest {

    @Test
    fun `wire is idempotent`() {
        val root = kotlin.io.path.createTempDirectory("android-wire").toFile()
        val moduleName = "profile"
        val pkg = "com.example.egs_android_template.feature.profile"
        val pkgPath = pkg.replace('.', '/')
        val kt = root.resolve(
            "feature/$moduleName/src/main/kotlin/$pkgPath/ProfileKoinModule.kt",
        )
        kt.parentFile.mkdirs()
        kt.writeText(
            """
            package $pkg

            import org.koin.core.module.Module
            import ${pkg}.data.dataModule
            import ${pkg}.domain.domainModule
            import ${pkg}.presentation.presentationModule

            val featureProfileModules: List<Module> = listOf(
                dataModule,
                domainModule,
                presentationModule,
            )
            """.trimIndent(),
        )

        val config = SubProjectConfig(
            platform = Platform.ANDROID,
            path = ".",
            basePackage = "com.example.egs_android_template",
            conventionPluginId = "com.example.convention.feature",
        )

        val updater = AndroidApiSyncKoinUpdater()
        updater.applyAfterSync(root, moduleName, config)
        val once = kt.readText()
        updater.applyAfterSync(root, moduleName, config)
        val twice = kt.readText()

        assertEquals(once, twice)
        assertTrue(once.contains("generatedDataModule,"))
        assertTrue(once.contains("generatedDomainModule,"))
        assertTrue(once.contains("import $pkg.generate.di.generatedDataModule"))
    }
}
