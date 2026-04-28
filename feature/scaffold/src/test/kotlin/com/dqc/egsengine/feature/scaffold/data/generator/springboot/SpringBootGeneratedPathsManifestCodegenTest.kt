/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SchemaTraitInferrer
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootCodegenModelBuilder
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SpringBootGeneratedPathsManifestCodegenTest {

    @Test
    fun writeThenRead_roundTripsCodegen() {
        val root = Files.createTempDirectory("egs-manifest-codegen").toFile()
        try {
            val sut = SpringBootGeneratedPathsManifest()
            val cols =
                listOf(
                    BackendCodegenManifestColumn(
                        kotlinName = "id",
                        kotlinType = "Long",
                        tsType = "number",
                        nullable = false,
                        isPk = true,
                        inBusinessForm = false,
                    ),
                    BackendCodegenManifestColumn(
                        kotlinName = "name",
                        kotlinType = "String",
                        tsType = "string",
                        nullable = false,
                        isPk = false,
                        inBusinessForm = true,
                    ),
                )
            val original =
                BackendCodegenManifest(
                    schemaVersion = 1,
                    entityPascal = "EgsDemo",
                    entityCamel = "egsDemo",
                    restPath = "demos",
                    tableSqlName = "egs_demos",
                    backendModuleName = "demos",
                    basePackage = "com.example.demo",
                    pkField = "id",
                    pkTsType = "number",
                    columns = cols,
                )
            sut.write(
                backendRoot = root,
                moduleName = "demos",
                tableName = "egs_demos",
                paths = listOf("feature/demos/generated/Foo.kt"),
                codegen = original,
            )

            val dto = sut.read(root, "demos")
            assertNotNull(dto)
            val read = dto!!.codegen
            assertNotNull(read)
            assertEquals(original.schemaVersion, read!!.schemaVersion)
            assertEquals(original.entityPascal, read.entityPascal)
            assertEquals(original.entityCamel, read.entityCamel)
            assertEquals(original.restPath, read.restPath)
            assertEquals(original.tableSqlName, read.tableSqlName)
            assertEquals(original.backendModuleName, read.backendModuleName)
            assertEquals(original.basePackage, read.basePackage)
            assertEquals(original.pkField, read.pkField)
            assertEquals(original.pkTsType, read.pkTsType)
            assertEquals(original.columns.size, read.columns.size)
            for (i in original.columns.indices) {
                val a = original.columns[i]
                val b = read.columns[i]
                assertEquals(a.kotlinName, b.kotlinName)
                assertEquals(a.kotlinType, b.kotlinType)
                assertEquals(a.tsType, b.tsType)
                assertEquals(a.nullable, b.nullable)
                assertEquals(a.isPk, b.isPk)
                assertEquals(a.inBusinessForm, b.inBusinessForm)
            }

            assertEquals(2, dto.version)
            assertEquals(listOf("feature/demos/generated/Foo.kt"), dto.generatedPaths)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun ddl_egsDemos_manifestMatchesSpringCodegen_restPathGolden() {
        val ddl =
            """
            CREATE TABLE egs_demos (
                id          BIGINT       NOT NULL AUTO_INCREMENT,
                name        VARCHAR(255) NOT NULL,
                PRIMARY KEY (id)
            );
            """.trimIndent()
        val tmp = Files.createTempFile("egs-", ".sql").toFile()
        try {
            tmp.writeText(ddl)
            val table = DdlParser().parseFile(tmp).single()
            val builder = SpringBootCodegenModelBuilder(SchemaTraitInferrer())
            val cfg =
                SubProjectConfig(
                    platform = Platform.SPRING_BOOT,
                    path = "backend",
                    basePackage = "com.example.demo",
                )
            val manifest =
                builder.buildBackendCodegenManifest(
                    table = table,
                    backendModuleName = "demos",
                    config = cfg,
                    options = SpringBootOpinionatedOptions(),
                )
            assertEquals("demos", manifest.restPath)
            assertEquals("EgDemo", manifest.entityPascal)
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun readCodegenOnly_matchesReadCodegen() {
        val root = Files.createTempDirectory("egs-manifest-codegen2").toFile()
        try {
            val sut = SpringBootGeneratedPathsManifest()
            val codegen =
                BackendCodegenManifest(
                    schemaVersion = 1,
                    entityPascal = "X",
                    entityCamel = "x",
                    restPath = "x-path",
                    tableSqlName = "t_x",
                    backendModuleName = "x",
                    basePackage = "p",
                    pkField = "id",
                    pkTsType = "number",
                    columns = emptyList(),
                )
            sut.write(root, "x", "t_x", emptyList(), codegen)
            val only = sut.readCodegenOnly(root, "x")
            assertEquals(codegen.restPath, only!!.restPath)
        } finally {
            root.deleteRecursively()
        }
    }
}
