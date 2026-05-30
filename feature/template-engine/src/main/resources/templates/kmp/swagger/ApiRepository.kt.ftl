package ${packageName}

<#list imports as imp>
import ${imp}
</#list>

internal interface ${apiRepositoryName} {
<#list operations as op>
<#if !op.params?has_content && op.hasBody>
    suspend fun ${op.operationId}(body: ${op.bodyType}): ${op.returnType}
<#else>
    suspend fun ${op.operationId}(
<#list op.params as param>
        ${param.name}: ${param.type}<#if param_has_next>,</#if>
</#list>
<#if op.hasBody>
<#if op.params?has_content>,</#if>
        body: ${op.bodyType}
</#if>
    ): ${op.returnType}
</#if>

</#list>
}
