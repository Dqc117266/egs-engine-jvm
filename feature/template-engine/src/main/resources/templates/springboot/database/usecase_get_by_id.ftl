${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}Repository

class Get${entityPascal}ByIdUseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(id: Long): ${entityPascal}? = repository.findById(id)
}
