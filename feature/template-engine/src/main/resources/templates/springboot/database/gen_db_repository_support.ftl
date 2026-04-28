${warningGenerated}
package ${generatePackage}.data.repository

import ${generatePackage}.data.datasource.jpa.entity.${entityPascal}Entity
import ${generatePackage}.data.datasource.jpa.${entityCamel}JpaRepository
import ${generatePackage}.data.mapper.Generated${entityPascal}EntityMapper
import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}DbRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

open class Generated${entityPascal}DbRepositorySupport(
    private val jpaRepository: ${entityCamel}JpaRepository,
    private val mapper: Generated${entityPascal}EntityMapper,
) : ${entityPascal}DbRepository {

    @Transactional(readOnly = true)
    override fun findById(id: Long): ${entityPascal}? =
        jpaRepository.findById(id).map { mapper.toDomain(it) }.orElse(null)

    @Transactional(readOnly = true)
    override fun findAll(pageable: Pageable): Page<${entityPascal}> =
        jpaRepository.findAll(pageable).map { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAll(): List<${entityPascal}> =
        jpaRepository.findAll().map { mapper.toDomain(it) }

    @Transactional
    override fun save(item: ${entityPascal}): ${entityPascal} {
        val entity: ${entityPascal}Entity =
            if (item.${pkProp} == 0L) {
                mapper.toNewEntity(item)
            } else {
                val existing =
                    jpaRepository.findById(item.${pkProp}).orElseThrow {
                        IllegalArgumentException("${entityPascal} not found: ${'$'}{item.${pkProp}}")
                    }
<#list entityBodyColumns as col>
                existing.${col.kotlinName} = item.${col.kotlinName}
</#list>
                existing
            }
        return mapper.toDomain(jpaRepository.save(entity))
    }

    @Transactional
    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    override fun existsById(id: Long): Boolean =
        jpaRepository.existsById(id)

    @Transactional(readOnly = true)
    override fun count(): Long =
        jpaRepository.count()
}
