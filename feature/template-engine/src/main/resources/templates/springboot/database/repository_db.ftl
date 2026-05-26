${warningGenerated}
package ${generatePackage}.domain.repository

import ${generatePackage}.domain.model.${entityPascal}
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ${entityPascal}DbRepository {
    fun findById(id: Long): ${entityPascal}?
    fun findAll(pageable: Pageable): Page<${entityPascal}>
    fun findAll(): List<${entityPascal}>
    fun save(item: ${entityPascal}): ${entityPascal}
    fun deleteById(id: Long)
    fun existsById(id: Long): Boolean
    fun count(): Long
}
