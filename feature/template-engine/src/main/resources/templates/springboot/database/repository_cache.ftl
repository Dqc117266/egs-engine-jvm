${warningGenerated}
package ${generatePackage}.domain.repository

import ${generatePackage}.domain.model.${entityPascal}

interface ${entityPascal}CacheRepository {
    fun findById(id: Long): ${entityPascal}?
    fun save(item: ${entityPascal})
    fun deleteById(id: Long)
    fun existsById(id: Long): Boolean
}
