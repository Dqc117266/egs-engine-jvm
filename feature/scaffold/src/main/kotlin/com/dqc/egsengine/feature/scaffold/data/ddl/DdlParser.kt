package com.dqc.egsengine.feature.scaffold.data.ddl

import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import org.slf4j.LoggerFactory

/**
 * Parses SQL CREATE TABLE statements into [TableSchema] models.
 *
 * Skeleton -- full SQL parsing to be implemented later.
 */
class DdlParser {

    private val logger = LoggerFactory.getLogger(DdlParser::class.java)

    fun parse(sql: String): List<TableSchema> {
        logger.info("DdlParser.parse() called -- not yet implemented")
        TODO("DDL parsing not yet implemented")
    }

    fun parseFile(file: java.io.File): List<TableSchema> {
        require(file.exists()) { "DDL file not found: ${file.absolutePath}" }
        return parse(file.readText())
    }
}
