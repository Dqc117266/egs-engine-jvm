package ${generateDiPackage}

import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ${dataRepositoryPackage}.${apiRepositorySupportName}
import ${servicePackage}.create${serviceName}

internal val generatedDataModule = module {
    single { get<Ktorfit>().create${serviceName}() }

    singleOf(::${apiRepositorySupportName})

    // egs-gen:database-begin

    // egs-gen:database-end
}
