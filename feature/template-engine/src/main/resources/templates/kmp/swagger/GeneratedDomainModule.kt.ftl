package ${generateDiPackage}

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
<#list swaggerUseCases as uc>
import ${uc.domainUseCasePackage}.${uc.useCaseClass}
</#list>
<#list dbUseCaseImports as imp>
import ${imp}
</#list>
<#list prefsUseCaseImports as imp>
import ${imp}
</#list>

internal val generatedDomainModule = module {
    // egs-gen:swagger-usecases-begin
<#list swaggerUseCases as uc>
    singleOf(::${uc.useCaseClass})
</#list>
    // egs-gen:swagger-usecases-end

    // egs-gen:db-usecases-begin
<#list dbUseCases as uc>
    singleOf(::${uc.useCaseClass})
</#list>
    // egs-gen:db-usecases-end

    // egs-gen:prefs-usecases-begin
<#list prefsUseCases as uc>
    singleOf(::${uc.useCaseClass})
</#list>
    // egs-gen:prefs-usecases-end
}
