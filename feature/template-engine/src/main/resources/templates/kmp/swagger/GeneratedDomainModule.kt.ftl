package ${generateRootPackage}

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
<#list useCases as uc>
import ${uc.domainUseCasePackage}.${uc.useCaseClass}
</#list>

internal val generatedDomainModule = module {
<#list useCases as uc>
    singleOf(::${uc.useCaseClass})
</#list>
}
