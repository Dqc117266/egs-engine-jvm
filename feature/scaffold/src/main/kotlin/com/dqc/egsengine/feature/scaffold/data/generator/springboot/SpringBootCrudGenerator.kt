package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.ddl.model.TableSchema
import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import org.slf4j.LoggerFactory

/**
 * DDL-driven CRUD code generator for Spring Boot.
 * Generates Entity, JpaRepository, Service, Controller, DTOs per table.
 *
 * Skeleton -- concrete KotlinPoet generation to be implemented later.
 */
class SpringBootCrudGenerator {

    private val logger = LoggerFactory.getLogger(SpringBootCrudGenerator::class.java)

    fun generate(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        logger.info("SpringBootCrudGenerator.generate() called for table '{}' -- not yet implemented", table.tableName)
        TODO("Spring Boot CRUD generation not yet implemented")
    }

    fun preview(
        table: TableSchema,
        moduleName: String,
        config: SubProjectConfig,
    ): List<GeneratedFile> {
        logger.info("SpringBootCrudGenerator.preview() called for table '{}' -- not yet implemented", table.tableName)
        TODO("Spring Boot CRUD preview not yet implemented")
    }
}
