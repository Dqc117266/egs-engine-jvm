package ${packageName}

<#list imports as imp>
import ${imp}
</#list>

internal interface ${repositoryName} {
<#list operations as op>
    suspend fun ${op.operationId}(
<#list op.params as param>
        ${param.name}: ${param.type}<#if param_has_next>,</#if>
</#list>
<#if op.hasBody>
<#if op.params?has_content>,</#if>
        body: ${op.bodyType}
</#if>
    ): ${op.returnType}

</#list>
}
