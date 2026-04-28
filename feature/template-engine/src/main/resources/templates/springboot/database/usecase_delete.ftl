${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.repository.${entityPascal}Repository

class Delete${entityPascal}ByIdUseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }
}
