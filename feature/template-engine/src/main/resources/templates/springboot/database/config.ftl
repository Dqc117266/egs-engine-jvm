${warningGenerated}
package ${generatePackage}.config

import ${generatePackage}.data.datasource.cache.${entityPascal}CacheDataSource
import ${generatePackage}.data.datasource.httpclient.${entityPascal}HttpClient
import ${generatePackage}.domain.repository.${entityPascal}Repository
import ${generatePackage}.domain.usecase.Count${entityPascal}UseCase
import ${generatePackage}.domain.usecase.Create${entityPascal}UseCase
import ${generatePackage}.domain.usecase.Delete${entityPascal}ByIdUseCase
import ${generatePackage}.domain.usecase.Get${entityPascal}AllUseCase
import ${generatePackage}.domain.usecase.Get${entityPascal}ByIdUseCase
import ${generatePackage}.domain.usecase.Get${entityPascal}PageUseCase
import ${generatePackage}.domain.usecase.Update${entityPascal}UseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.client.RestClient

@Configuration
open class Generated${entityPascal}Config {

    @Bean
    @ConditionalOnMissingBean(${entityPascal}CacheDataSource::class)
    open fun ${entityCamel}CacheDataSource(redis: RedisTemplate<String, Any>): ${entityPascal}CacheDataSource =
        ${entityPascal}CacheDataSource(redis)

    @Bean
    @ConditionalOnMissingBean(${entityPascal}HttpClient::class)
    open fun ${entityCamel}HttpClient(
${configHttpClientParamLine}
    ): ${entityPascal}HttpClient = ${entityPascal}HttpClient(RestClient.builder().build(), baseUrl)

    @Bean
    open fun create${entityPascal}UseCase(${entityCamel}Repository: ${entityPascal}Repository): Create${entityPascal}UseCase =
        Create${entityPascal}UseCase(${entityCamel}Repository)

    @Bean
    open fun update${entityPascal}UseCase(${entityCamel}Repository: ${entityPascal}Repository): Update${entityPascal}UseCase =
        Update${entityPascal}UseCase(${entityCamel}Repository)

    @Bean
    open fun delete${entityPascal}ByIdUseCase(${entityCamel}Repository: ${entityPascal}Repository): Delete${entityPascal}ByIdUseCase =
        Delete${entityPascal}ByIdUseCase(${entityCamel}Repository)

    @Bean
    open fun get${entityPascal}ByIdUseCase(${entityCamel}Repository: ${entityPascal}Repository): Get${entityPascal}ByIdUseCase =
        Get${entityPascal}ByIdUseCase(${entityCamel}Repository)

    @Bean
    open fun get${entityPascal}PageUseCase(${entityCamel}Repository: ${entityPascal}Repository): Get${entityPascal}PageUseCase =
        Get${entityPascal}PageUseCase(${entityCamel}Repository)

    @Bean
    open fun get${entityPascal}AllUseCase(${entityCamel}Repository: ${entityPascal}Repository): Get${entityPascal}AllUseCase =
        Get${entityPascal}AllUseCase(${entityCamel}Repository)

    @Bean
    open fun count${entityPascal}UseCase(${entityCamel}Repository: ${entityPascal}Repository): Count${entityPascal}UseCase =
        Count${entityPascal}UseCase(${entityCamel}Repository)
}
