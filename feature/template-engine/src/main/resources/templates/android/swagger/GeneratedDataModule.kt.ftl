package ${generateDiPackage}

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import retrofit2.Retrofit
import ${servicePackage}.${serviceName}
import ${dataRepositoryPackage}.${apiRepositorySupportName}

internal val generatedDataModule = module {
    single { get<Retrofit>().create(${serviceName}::class.java) }

    singleOf(::${apiRepositorySupportName})

    // egs-gen:database-begin

    // egs-gen:database-end

    // egs-gen:prefs-begin

    // egs-gen:prefs-end
}
