package ${packageName}

import org.koin.core.module.Module
import ${packageName}.presentation.presentationModule
import ${packageName}.domain.domainModule
import ${packageName}.data.dataModule

val feature${pascal}Modules: List<Module> = listOf(
    presentationModule,
    domainModule,
    dataModule,
)
