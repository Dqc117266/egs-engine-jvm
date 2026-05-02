package com.dqc.egsengine.feature.scaffold.data.generator.springboot

import com.dqc.egsengine.feature.init.domain.model.Platform
import com.dqc.egsengine.feature.init.domain.model.SubProjectConfig
import com.dqc.egsengine.feature.scaffold.data.ddl.DdlParser
import com.dqc.egsengine.feature.scaffold.data.generator.springboot.database.SpringBootOpinionatedOptions
import com.dqc.egsengine.template.TemplateEngine
import com.dqc.egsengine.template.TemplateRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SpringBootCrudGeneratorTemplateRegressionTest {

    @Test
    fun `generate handles nullable ints and large varchar lengths without invalid Kotlin`() {
        val ddl =
            """
            CREATE TABLE recipe_step (
                id BIGSERIAL PRIMARY KEY,
                recipe_category_id BIGINT NOT NULL,
                step_number INT NOT NULL,
                title VARCHAR(100) NOT NULL,
                description TEXT,
                image_url VARCHAR(500),
                tts_text VARCHAR(1000),
                duration_minutes INT,
                tips VARCHAR(500)
            );
            """.trimIndent()
        val sqlFile = Files.createTempFile("recipe-step-", ".sql").toFile()
        try {
            sqlFile.writeText(ddl)
            val table = DdlParser().parseFile(sqlFile).single()
            val generator = SpringBootCrudGenerator(TemplateEngine(TemplateRegistry()))
            val config =
                SubProjectConfig(
                    platform = Platform.SPRING_BOOT,
                    path = "backend",
                    basePackage = "com.egs.server",
                    conventionPluginId = "com.egs.server.convention.feature",
                )

            val files = generator.generate(table, "recipe", config, SpringBootOpinionatedOptions(), null)
            val requestDtos = files.first { it.path.endsWith("RecipeStepRequestDtos.kt") }.content.orEmpty()
            val entity = files.first { it.path.endsWith("RecipeStepEntity.kt") }.content.orEmpty()

            assertTrue(requestDtos.contains("@field:Size(max = 1000)"))
            assertFalse(requestDtos.contains("@field:Size(max = 1,000)"))
            assertTrue(requestDtos.contains("val durationMinutes: Int? = null"))
            assertTrue(entity.contains("@Column(nullable = true, length = 1000)"))
            assertFalse(entity.contains("length = 1,000"))
            assertTrue(entity.contains("var durationMinutes: Int? = null"))
        } finally {
            sqlFile.delete()
        }
    }
}
