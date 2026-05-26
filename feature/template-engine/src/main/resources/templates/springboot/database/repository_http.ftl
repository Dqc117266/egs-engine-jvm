${warningGenerated}
package ${generatePackage}.domain.repository

import ${generatePackage}.domain.model.${entityPascal}

interface ${entityPascal}HttpRepository {
    fun findById(id: Long): ${entityPascal}?
}
