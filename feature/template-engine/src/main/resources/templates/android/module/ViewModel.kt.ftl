package ${packageName}.presentation.screen

<#if hasBaseViewModel>
import ${baseViewModelImport}
<#else>
import androidx.lifecycle.ViewModel
</#if>

<#if hasBaseViewModel>
internal class ${pascal}ViewModel : ${baseViewModelSimpleName}<${pascal}Contract.State, ${pascal}Contract.Intent, ${pascal}Contract.Effect>(
    ${pascal}Contract.State(
        isLoading = false,
        error = null,
    ),
) {
    override fun registerIntents() {
    }
}
<#else>
internal class ${pascal}ViewModel : ViewModel()
</#if>
