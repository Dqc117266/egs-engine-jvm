package ${dataPackage}

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.bind
import retrofit2.Retrofit
import ${dataRepositoryPackage}.${repositoryImplName}
import ${domainRepositoryPackage}.${apiRepositoryName}
import ${servicePackage}.${serviceName}

internal val dataModule = module {
    singleOf(::${repositoryImplName}) { bind<${apiRepositoryName}>() }
    single { get<Retrofit>().create(${serviceName}::class.java) }
}
