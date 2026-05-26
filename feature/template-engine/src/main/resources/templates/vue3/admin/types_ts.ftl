/** Response shape from backend codegen manifest */

export interface ${entityPascal}Item {
<#list columns as col>
  ${col.kotlinName}<#if col.nullable && !col.pk>?</#if>: ${col.tsType}<#if col_has_next>,</#if>
</#list>
}

export interface ${entityPascal}CreateBody {
<#list formColumns as col>
  ${col.kotlinName}<#if col.nullable || (col.kotlinType == "Int")>?</#if>: ${col.tsType}<#if col_has_next>,</#if>
</#list>
}

export type ${entityPascal}UpdateBody = Partial<${entityPascal}CreateBody>
