package ${generateDiPackage}

import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.dsl.module
import ${servicePackage}.create${serviceName}

internal val generatedDataModule = module {
    single { get<Ktorfit>().create${serviceName}() }

    // egs-gen:database-begin

    // egs-gen:database-end
}
