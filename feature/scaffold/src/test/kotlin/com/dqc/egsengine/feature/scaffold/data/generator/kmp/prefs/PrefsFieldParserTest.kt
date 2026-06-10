package com.dqc.egsengine.feature.scaffold.data.generator.kmp.prefs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PrefsFieldParserTest {
    @Test
    fun `parses comma separated fields with bool alias`() {
        val f = PrefsFieldParser.parseFields(" userId:String , isLogin:bool ")
        assertEquals(2, f.size)
        assertEquals("userId", f[0].name)
        assertEquals("Boolean", f[1].kotlinType)
        assertEquals(PrefsStorageKind.BOOLEAN, f[1].storageKind)
    }

    @Test
    fun `rejects unknown type`() {
        assertThrows(IllegalArgumentException::class.java) {
            PrefsFieldParser.parseFields("x:Float")
        }
    }
}
