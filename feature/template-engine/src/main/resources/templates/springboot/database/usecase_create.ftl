${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}Repository

class Create${entityPascal}UseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(item: ${entityPascal}): ${entityPascal} = repository.save(item.copy(${pkProp} = 0L))
}
