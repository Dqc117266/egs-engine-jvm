${warningGenerated}
package ${generatePackage}.data.repository

import ${generatePackage}.data.datasource.httpclient.${entityPascal}HttpClient
import ${generatePackage}.data.mapper.Generated${entityPascal}HttpMapper
import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}HttpRepository

open class Generated${entityPascal}HttpRepositorySupport(
    private val client: ${entityPascal}HttpClient,
    private val mapper: Generated${entityPascal}HttpMapper,
) : ${entityPascal}HttpRepository {

    override fun findById(id: Long): ${entityPascal}? {
        val remote = client.fetchById(id) ?: return null
        return mapper.toDomain(remote)
    }
}
