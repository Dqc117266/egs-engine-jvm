package ${packageName}.data

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import ${packageName}.data.repository.${pascal}RepositoryImpl
import ${packageName}.domain.repository.${pascal}Repository

internal val dataModule = module {

    singleOf(::${pascal}RepositoryImpl) { bind<${pascal}Repository>() }
}
