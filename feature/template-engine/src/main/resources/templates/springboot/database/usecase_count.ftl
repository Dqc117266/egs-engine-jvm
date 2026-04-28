${warningGenerated}
package ${generatePackage}.domain.usecase

import ${generatePackage}.domain.repository.${entityPascal}Repository

class Count${entityPascal}UseCase(
    private val repository: ${entityPascal}Repository,
) {
    operator fun invoke(): Long = repository.count()
}
