${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}Repository

class Get${entityPascal}AllUseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(): List<${entityPascal}> = repository.findAll()
}
