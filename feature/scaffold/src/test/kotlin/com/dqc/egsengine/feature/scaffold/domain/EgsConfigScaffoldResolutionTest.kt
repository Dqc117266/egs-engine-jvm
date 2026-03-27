package com.dqc.egsengine.feature.scaffold.domain

import com.dqc.egsengine.feature.init.domain.model.BaseClassInfo
import com.dqc.egsengine.feature.init.domain.model.BaseClassKind
import com.dqc.egsengine.feature.init.domain.model.EgsConfig
import com.dqc.egsengine.feature.init.domain.model.ModuleStructure
import com.dqc.egsengine.feature.init.domain.model.ScaffoldOverrides
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EgsConfigScaffoldResolutionTest {

    private fun minimalConfig(
        basePackage: String? = null,
        baseClasses: List<BaseClassInfo> = emptyList(),
        overrides: ScaffoldOverrides? = null,
    ) = EgsConfig(
        projectName = "p",
        projectType = "KMP",
        rootPath = "/tmp",
        basePackage = basePackage,
        moduleStructure = ModuleStructure(layers = listOf("domain"), hasRes = false),
        baseClasses = baseClasses,
        scaffoldOverrides = overrides,
    )

    @Test
    fun `effectiveBasePackage prefers scaffoldOverrides`() {
        val c = minimalConfig(basePackage = "com.detected", overrides = ScaffoldOverrides(basePackage = "template"))
        assertEquals("template", c.effectiveBasePackage())
    }

    @Test
    fun `resolveScaffoldBaseClasses prefers FQN overrides over scanned list`() {
        val scanned = BaseClassInfo(
            name = "BaseViewModel",
            packageName = "wrong.pkg",
            module = ":m",
            filePath = "x.kt",
            kind = BaseClassKind.ABSTRACT_CLASS,
        )
        val c = minimalConfig(
            baseClasses = listOf(scanned),
            overrides = ScaffoldOverrides(baseViewModelFqn = "template.core.base.ui.BaseViewModel"),
        )
        assertEquals(
            "template.core.base.ui.BaseViewModel",
            c.resolveScaffoldBaseClasses(includeRetrofitProvider = false).baseViewModel,
        )
    }

    @Test
    fun `resolveScaffoldBaseClasses omits retrofit when flag false`() {
        val c = minimalConfig(basePackage = "com.example")
        assertNull(c.resolveScaffoldBaseClasses(includeRetrofitProvider = false).retrofitProvider)
    }

    @Test
    fun `resolveScaffoldBaseClasses includes retrofit when flag true`() {
        val c = minimalConfig(basePackage = "com.example")
        assertEquals(
            "com.example.feature.common.network.DynamicRetrofitProvider",
            c.resolveScaffoldBaseClasses(includeRetrofitProvider = true).retrofitProvider,
        )
    }
}
