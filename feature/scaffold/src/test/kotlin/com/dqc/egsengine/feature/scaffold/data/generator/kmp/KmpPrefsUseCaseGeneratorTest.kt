package com.dqc.egsengine.feature.scaffold.data.generator.kmp

import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFieldParser
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationModeResolver
import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.feature.templateengine.TemplateEngine
import com.dqc.egsengine.feature.templateengine.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KmpPrefsUseCaseGeneratorTest {
    @Test
    fun `generates Get Set Observe use cases for scalar prefs`() {
        val engine = TemplateEngine(TemplateRegistry())
        val gen = KmpPrefsUseCaseGenerator(engine)
        val fields = PrefsFieldParser.parseFields("userId:String")
        val mode = PrefsGenerationModeResolver.resolve(null, fields)
        val template =
            ModuleTemplate(
                name = "todo",
                packageName = "com.example.feature.todo",
                conventionPluginId = null,
                layers = listOf("data", "domain", "presentation"),
                hasRes = false,
                namespace = null,
                projectType = "KMP",
                basePackage = "com.example",
                baseClassPackages =
                BaseClassPackages(
                    resultClass = "com.example.feature.base.domain.result.Result",
                ),
            )
        val files = gen.generate(template, mode, projectRoot = null, subProjectRoot = null)
        val paths = files.map { it.path }
        assertTrue(paths.any { it.endsWith("/GetUserIdUseCase.kt") })
        assertTrue(paths.any { it.endsWith("/SetUserIdUseCase.kt") })
        assertTrue(paths.any { it.endsWith("/ObserveUserIdUseCase.kt") })
        assertTrue(paths.any { it.endsWith("/GeneratedDomainModule.kt") })

        val observe = files.first { it.path.endsWith("/ObserveUserIdUseCase.kt") }.content!!
        assertTrue(observe.contains("operator fun invoke()"))
        assertTrue(observe.contains("Flow<String>"))
        assertTrue(observe.contains("observeUserId()"))
    }
}
