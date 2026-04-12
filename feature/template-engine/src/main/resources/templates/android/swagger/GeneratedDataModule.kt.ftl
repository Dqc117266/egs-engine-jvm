package ${generateDiPackage}

import org.koin.dsl.module
import retrofit2.Retrofit
import ${servicePackage}.${serviceName}

internal val generatedDataModule = module {
    single { get<Retrofit>().create(${serviceName}::class.java) }
}
