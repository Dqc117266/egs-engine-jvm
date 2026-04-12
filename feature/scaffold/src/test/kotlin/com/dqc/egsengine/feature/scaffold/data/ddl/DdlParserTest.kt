package com.dqc.egsengine.feature.scaffold.data.ddl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DdlParserTest {

    private val parser = DdlParser()

    @Test
    fun `parses user sql tables and columns`() {
        val url = javaClass.classLoader.getResource("ddl/user.sql")
            ?: error("ddl/user.sql not on test classpath")
        val tables = parser.parseFile(java.io.File(url.toURI()))

        assertEquals(2, tables.size)

        val user = tables.find { it.tableName == "user" }!!
        assertEquals("id", user.primaryKey)
        val userCols = user.columns.associateBy { it.name }
        assertEquals("Long", userCols["id"]!!.kotlinType)
        assertTrue(userCols["id"]!!.isPrimaryKey)
        assertTrue(userCols["id"]!!.isAutoIncrement)
        assertFalse(userCols["id"]!!.nullable)
        assertEquals("String", userCols["username"]!!.kotlinType)
        assertFalse(userCols["username"]!!.nullable)
        assertTrue(userCols["avatar_url"]!!.nullable)
        assertEquals("Long", userCols["created_at"]!!.kotlinType)
        assertEquals("0", userCols["created_at"]!!.defaultValue)

        val session = tables.find { it.tableName == "user_session" }!!
        assertEquals("id", session.primaryKey)
        assertEquals("Long", session.columns.first { it.name == "user_id" }.kotlinType)
    }
}
