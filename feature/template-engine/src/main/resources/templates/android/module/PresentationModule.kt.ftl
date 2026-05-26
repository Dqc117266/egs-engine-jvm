package ${packageName}.presentation

import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModelOf
import ${packageName}.presentation.screen.${pascal}ViewModel

internal val presentationModule = module {
    viewModelOf(::${pascal}ViewModel)
}
