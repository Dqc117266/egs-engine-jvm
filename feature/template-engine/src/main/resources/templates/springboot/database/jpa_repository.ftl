${warningGenerated}
package ${generatePackage}.data.datasource.jpa

import ${generatePackage}.data.datasource.jpa.entity.${entityPascal}Entity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ${entityCamel}JpaRepository : JpaRepository<${entityPascal}Entity, Long> {
<#if jpaFinderNameColumn??>
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<${entityPascal}Entity>

</#if>
}
