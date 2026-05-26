${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}Repository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

class Get${entityPascal}PageUseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(pageable: Pageable): Page<${entityPascal}> = repository.findAll(pageable)
}
