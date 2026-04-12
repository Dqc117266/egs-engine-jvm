package ${domainPackage}

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
<#list useCases as uc>
import ${uc.domainUseCasePackage}.${uc.useCaseClass}
</#list>

internal val domainModule = module {
<#list useCases as uc>
    singleOf(::${uc.useCaseClass})
</#list>
}
