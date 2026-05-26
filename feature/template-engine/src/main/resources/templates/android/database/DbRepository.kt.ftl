/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${repositoryPackageName}

<#list rowImports as imp>
import ${imp}
</#list>

/**
 * DB repository slice. Methods mirror [${moduleDatabaseName}DataSource] delegates per table.
 */
internal interface ${dbRepositoryName} {
<#list tables as t>
    // --- ${t.sqlTableName} ---
    suspend fun get${t.prefixPascal}All(): List<${t.exposedRowType}>

    suspend fun get${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType}): ${t.exposedRowType}?

    suspend fun count${t.prefixPascal}(): Long

    suspend fun insert${t.prefixPascal}(entity: ${t.exposedRowType})

    suspend fun insertAll${t.prefixPascal}(entities: List<${t.exposedRowType}>)

    suspend fun update${t.prefixPascal}(entity: ${t.exposedRowType})

    suspend fun delete${t.prefixPascal}(entity: ${t.exposedRowType})

    suspend fun delete${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType})

    suspend fun deleteAll${t.prefixPascal}()

</#list>
}
