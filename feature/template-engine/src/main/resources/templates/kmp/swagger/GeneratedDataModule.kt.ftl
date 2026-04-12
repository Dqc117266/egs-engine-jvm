package ${generateRootPackage}

import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.dsl.module
import ${servicePackage}.create${serviceName}

internal val generatedDataModule = module {
    single { get<Ktorfit>().create${serviceName}() }
}
