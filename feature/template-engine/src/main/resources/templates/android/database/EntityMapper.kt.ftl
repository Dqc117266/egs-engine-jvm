/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${mapperPackageName}

<#list entityImports as imp>
import ${imp}
</#list>
<#list domainImports as imp>
import ${imp}
</#list>

<#list mapperBlocks as block>

fun ${block.entityClassName}.toDomain(): ${block.domainClassName} = ${block.domainClassName}(
${block.toDomainBody}
)

fun ${block.domainClassName}.toEntity(): ${block.entityClassName} = ${block.entityClassName}(
${block.toEntityBody}
)
</#list>
