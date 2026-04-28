${warningGenerated}
package ${generatePackage}.data.repository

import ${generatePackage}.data.datasource.cache.${entityPascal}CacheDataSource
import ${generatePackage}.data.datasource.cache.key.${entityPascal}CacheKeys
import ${generatePackage}.data.mapper.Generated${entityPascal}CacheMapper
import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}CacheRepository

open class Generated${entityPascal}CacheRepositorySupport(
    private val cache: ${entityPascal}CacheDataSource,
    private val mapper: Generated${entityPascal}CacheMapper,
) : ${entityPascal}CacheRepository {

    override fun findById(id: Long): ${entityPascal}? {
        val key = ${entityPascal}CacheKeys.entityKey(id)
        val payload = cache.get(key) ?: return null
        return mapper.toDomain(payload)
    }

    override fun save(item: ${entityPascal}) {
        require(item.${pkProp} != 0L) { "Cache save requires a persisted id" }
        val key = ${entityPascal}CacheKeys.entityKey(item.${pkProp})
        cache.put(key, mapper.toCacheModel(item))
    }

    override fun deleteById(id: Long) {
        cache.delete(${entityPascal}CacheKeys.entityKey(id))
    }

    override fun existsById(id: Long): Boolean = findById(id) != null
}
