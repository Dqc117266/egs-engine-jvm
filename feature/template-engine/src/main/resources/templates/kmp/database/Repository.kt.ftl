/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${repositoryPackageName}

<#list entityImports as imp>
import ${imp}
</#list>

/**
 * DB-only repository contract (Mode A). Methods mirror [${moduleDatabaseName}DataSource] delegates per table.
 */
internal interface ${repositoryName} {
<#list tables as t>
    // --- ${t.sqlTableName} ---
    suspend fun get${t.prefixPascal}All(): List<${t.entityClassName}>

    suspend fun get${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType}): ${t.entityClassName}?

    suspend fun count${t.prefixPascal}(): Long

    suspend fun insert${t.prefixPascal}(entity: ${t.entityClassName})

    suspend fun insertAll${t.prefixPascal}(entities: List<${t.entityClassName}>)

    suspend fun update${t.prefixPascal}(entity: ${t.entityClassName})

    suspend fun delete${t.prefixPascal}(entity: ${t.entityClassName})

    suspend fun delete${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType})

    suspend fun deleteAll${t.prefixPascal}()

</#list>
}
