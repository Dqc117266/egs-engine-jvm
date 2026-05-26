${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.model.${entityPascal}
import ${generatePackage}.domain.repository.${entityPascal}Repository

class Update${entityPascal}UseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(item: ${entityPascal}): ${entityPascal} {
        require(item.${pkProp} != 0L) { "${entityPascal} ${pkProp} is required for update" }
        return repository.save(item)
    }
}
