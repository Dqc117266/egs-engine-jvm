package com.dqc.egsengine.feature.init.data

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class BaseClassScannerKmpIntegrationTest {

    @Test
    fun `scans BaseViewModel from commonMain in sibling egs-kmp-template`() {
        val templateRoot = resolveEgsKmpTemplate()
        assumeTrue(templateRoot != null && templateRoot.isDirectory, "egs-kmp-template not found next to egs-engine-jvm")

        val scanner = BaseClassScannerImpl()
        val baseClasses = scanner.scan(templateRoot!!, listOf(":core-base:ui"))

        val vm = baseClasses.find { it.name == "BaseViewModel" }
        assertTrue(vm != null, "Expected BaseViewModel in :core-base:ui")
        assertTrue(vm!!.packageName == "template.core.base.ui", "package was ${vm.packageName}")
    }

    private fun resolveEgsKmpTemplate(): File? {
        System.getenv("EGS_KMP_TEMPLATE_ROOT")?.let { File(it) }?.takeIf { it.isDirectory }?.let { return it }
        val cwd = File(System.getProperty("user.dir"))
        return listOf(
            cwd.resolve("../egs-kmp-template"),
            cwd.resolve("../../egs-kmp-template"),
            cwd.resolve("egs-kmp-template"),
        )
            .map { it.normalize() }
            .firstOrNull { it.isDirectory }
    }
}
