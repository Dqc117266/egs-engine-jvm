/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${databasePackageName}

<#list tables as t>
import ${t.daoPackageName}.${t.daoClassName}
</#list>

/**
 * Thin wrapper over generated Room DAOs for module [${moduleDatabaseName}].
 */
internal class ${moduleDatabaseName}DataSource(
<#list tables as t>
    private val ${t.daoPropertyName}: ${t.daoClassName}<#if t?has_next>,</#if>
</#list>
)
