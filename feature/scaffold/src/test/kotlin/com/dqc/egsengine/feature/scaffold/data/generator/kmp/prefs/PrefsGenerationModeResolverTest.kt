package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrefsGenerationModeResolverTest {

    @Test
    fun `single field without key uses camel to snake logical key`() {
        val fields = PrefsFieldParser.parseFields("userId:String")
        val mode = PrefsGenerationModeResolver.resolve(null, fields)
        assertTrue(mode is PrefsGenerationMode.Scalar)
        val s = mode as PrefsGenerationMode.Scalar
        assertEquals("user_id", s.logicalKey)
    }

    @Test
    fun `multiple fields require explicit key`() {
        val fields = PrefsFieldParser.parseFields("a:String,b:String")
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            PrefsGenerationModeResolver.resolve(null, fields)
        }
    }

    @Test
    fun `snapshot class name from key`() {
        val fields = PrefsFieldParser.parseFields("userId:String,token:String")
        val mode = PrefsGenerationModeResolver.resolve("my_config", fields)
        assertTrue(mode is PrefsGenerationMode.Snapshot)
        assertEquals("MyConfig", (mode as PrefsGenerationMode.Snapshot).snapshotClassName)
    }
}
