package com.dqc.egsengine.feature.scaffold.data.generator.android

import com.dqc.egsengine.feature.scaffold.domain.model.BaseClassPackages
import com.dqc.egsengine.feature.scaffold.domain.model.ModuleTemplate
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsFieldParser
import com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs.PrefsGenerationModeResolver
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AndroidPrefsUseCaseGeneratorTest {

    @Test
    fun `generates Get Set Observe use cases under main kotlin`() {
        val engine = TemplateEngine(TemplateRegistry())
        val gen = AndroidPrefsUseCaseGenerator(engine)
        val fields = PrefsFieldParser.parseFields("userId:String")
        val mode = PrefsGenerationModeResolver.resolve(null, fields)
        val template = ModuleTemplate(
            name = "todo",
            packageName = "com.example.feature.todo",
            conventionPluginId = null,
            layers = listOf("data", "domain", "presentation"),
            hasRes = false,
            namespace = null,
            projectType = "ANDROID",
            basePackage = "com.example",
            baseClassPackages = BaseClassPackages(
                resultClass = "com.example.feature.base.domain.result.Result",
            ),
        )
        val files = gen.generate(template, mode, projectRoot = null, subProjectRoot = null)
        val paths = files.map { it.path }
        assertTrue(paths.any { it.contains("/src/main/kotlin/") && it.endsWith("/GetUserIdUseCase.kt") })
        assertTrue(paths.any { it.endsWith("/SetUserIdUseCase.kt") })
        assertTrue(paths.any { it.endsWith("/ObserveUserIdUseCase.kt") })
        assertTrue(paths.any { it.endsWith("/generate/di/GeneratedDomainModule.kt") })
    }
}
