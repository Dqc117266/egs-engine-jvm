package com.dqc.egsengine.feature.init.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GradleSourceRootsTest {

    @Test
    fun `orderedKotlinRoots prefers commonMain over androidMain and main`(@TempDir temp: File) {
        val module = temp.resolve("mod").apply { mkdirs() }
        module.resolve("src/androidMain/kotlin").mkdirs()
        module.resolve("src/main/kotlin").mkdirs()
        module.resolve("src/commonMain/kotlin").mkdirs()

        val ordered = GradleSourceRoots.orderedKotlinRoots(module).map { it.parentFile.name }
        assertEquals(listOf("commonMain", "main", "androidMain"), ordered)
    }

    @Test
    fun `hasAndroidStyleRes detects res under any source set`(@TempDir temp: File) {
        val module = temp.resolve("m").apply { mkdirs() }
        assertTrue(!GradleSourceRoots.hasAndroidStyleRes(module))

        module.resolve("src/androidMain/res/values").mkdirs()
        assertTrue(GradleSourceRoots.hasAndroidStyleRes(module))
    }
}
