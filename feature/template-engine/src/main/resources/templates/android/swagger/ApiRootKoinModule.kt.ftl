package ${rootPackage}

import org.koin.core.module.Module
import ${rootPackage}.presentation.presentationModule
import ${domainPackage}.domainModule
import ${dataPackage}.dataModule

val feature${pascalModuleName}Modules: List<Module> = listOf(
    presentationModule,
    domainModule,
    dataModule,
)
