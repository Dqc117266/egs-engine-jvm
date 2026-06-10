/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.generator.springboot.database

import com.dqc.egsengine.feature.scaffold.data.generator.common.GeneratedFile
import com.dqc.egsengine.template.TemplateEngine
import java.io.File

/**
 * Renders Kotlin sources under feature `generate` using FreeMarker templates in
 * `templates/springboot/database/`.
 */
class SpringBootCrudTemplateRenderer(
    private val templateEngine: TemplateEngine,
) {
    fun renderGeneratedSources(
        model: Map<String, Any?>,
        projectRoot: File?,
    ): List<GeneratedFile> {
        val ep = model["entityPascal"] as String
        val ec = model["entityCamel"] as String
        val moduleName = model["moduleName"] as String
        val featurePath = model["featurePackagePath"] as String
        fun rel(fileUnderFeature: String): String = "feature/$moduleName/src/main/kotlin/$featurePath/$fileUnderFeature"

        val pairs: List<Pair<String, String>> = buildList {
            add("springboot/database/domain_model.ftl" to rel("generate/domain/model/$ep.kt"))
            add("springboot/database/jpa_entity.ftl" to rel("generate/data/datasource/jpa/entity/${ep}Entity.kt"))
            add("springboot/database/jpa_repository.ftl" to rel("generate/data/datasource/jpa/${ec}JpaRepository.kt"))
            add("springboot/database/repository_facade.ftl" to rel("generate/domain/repository/${ep}Repository.kt"))
            add("springboot/database/repository_db.ftl" to rel("generate/domain/repository/${ep}DbRepository.kt"))
            add("springboot/database/repository_cache.ftl" to rel("generate/domain/repository/${ep}CacheRepository.kt"))
            add("springboot/database/repository_http.ftl" to rel("generate/domain/repository/${ep}HttpRepository.kt"))
            add("springboot/database/gen_db_repository_support.ftl" to rel("generate/data/repository/Generated${ep}DbRepositorySupport.kt"))
            add("springboot/database/gen_cache_repository_support.ftl" to rel("generate/data/repository/Generated${ep}CacheRepositorySupport.kt"))
            add("springboot/database/gen_http_repository_support.ftl" to rel("generate/data/repository/Generated${ep}HttpRepositorySupport.kt"))
            add("springboot/database/mapper_entity.ftl" to rel("generate/data/mapper/Generated${ep}EntityMapper.kt"))
            add("springboot/database/mapper_cache.ftl" to rel("generate/data/mapper/Generated${ep}CacheMapper.kt"))
            add("springboot/database/mapper_http.ftl" to rel("generate/data/mapper/Generated${ep}HttpMapper.kt"))
            add("springboot/database/cache_datasource.ftl" to rel("generate/data/datasource/cache/${ep}CacheDataSource.kt"))
            add("springboot/database/cache_keys.ftl" to rel("generate/data/datasource/cache/key/${ep}CacheKeys.kt"))
            add("springboot/database/cache_model.ftl" to rel("generate/data/datasource/cache/model/${ep}CacheModel.kt"))
            add("springboot/database/http_client.ftl" to rel("generate/data/datasource/httpclient/${ep}HttpClient.kt"))
            add("springboot/database/http_model.ftl" to rel("generate/data/datasource/httpclient/model/${ep}RemoteItemHttpModel.kt"))
            add("springboot/database/dto_requests.ftl" to rel("generate/api/dto/${ep}RequestDtos.kt"))
            add("springboot/database/dto_responses.ftl" to rel("generate/api/dto/${ep}ResponseDtos.kt"))
            add("springboot/database/controller.ftl" to rel("generate/api/controller/Generated${ep}Controller.kt"))
            add("springboot/database/config.ftl" to rel("generate/config/Generated${ep}Config.kt"))
            add("springboot/database/usecase_create.ftl" to rel("generate/domain/usecase/Create${ep}UseCase.kt"))
            add("springboot/database/usecase_update.ftl" to rel("generate/domain/usecase/Update${ep}UseCase.kt"))
            add("springboot/database/usecase_delete.ftl" to rel("generate/domain/usecase/Delete${ep}ByIdUseCase.kt"))
            add("springboot/database/usecase_get_by_id.ftl" to rel("generate/domain/usecase/Get${ep}ByIdUseCase.kt"))
            add("springboot/database/usecase_get_page.ftl" to rel("generate/domain/usecase/Get${ep}PageUseCase.kt"))
            add("springboot/database/usecase_get_all.ftl" to rel("generate/domain/usecase/Get${ep}AllUseCase.kt"))
            add("springboot/database/usecase_count.ftl" to rel("generate/domain/usecase/Count${ep}UseCase.kt"))
        }
        return pairs.map { (tpl, path) ->
            val content = templateEngine.render(tpl, model, projectRoot)
            GeneratedFile(path = path, content = content)
        }
    }
}
