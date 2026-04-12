package ${packageName}

import org.koin.core.module.Module
import ${packageName}.data.dataModule
import ${packageName}.domain.domainModule
import ${packageName}.presentation.presentationModule

val feature${pascal}Modules: List<Module> = listOf(
    dataModule,
    domainModule,
    presentationModule,
)
