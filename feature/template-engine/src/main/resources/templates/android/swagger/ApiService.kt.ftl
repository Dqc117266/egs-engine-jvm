package ${packageName}

<#list imports as imp>
import ${imp}
</#list>

internal interface ${serviceName} {
<#list operations as op>
    @${op.methodAnnotationSimple}("${op.path}")
    suspend fun ${op.operationId}(
<#list op.params as param>
        <#if param.pathAnnotation>@Path("${param.originalName}")<#else>@Query("${param.originalName}")</#if> ${param.name}: ${param.type}<#if param_has_next>,</#if>
</#list>
<#if op.hasBody>
<#if op.params?has_content>,</#if>
        @Body body: ${op.bodyType}
</#if>
    ): ${op.returnType}

</#list>
}
