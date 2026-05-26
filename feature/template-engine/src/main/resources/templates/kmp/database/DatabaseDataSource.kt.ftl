/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${databasePackageName}

<#list tables as t>
import ${t.entityPackageName}.${t.entityClassName}
import ${t.daoPackageName}.${t.daoClassName}
</#list>

/**
 * Thin wrapper over generated Room DAOs for module [${moduleDatabaseName}] (delegates CRUD to each DAO).
 */
internal class ${moduleDatabaseName}DataSource(
<#list tables as t>
    private val ${t.daoPropertyName}: ${t.daoClassName}<#if t?has_next>,</#if>
</#list>
) {
<#list tables as t>

    suspend fun get${t.prefixPascal}All() = ${t.daoPropertyName}.getAll()

    suspend fun get${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType}) = ${t.daoPropertyName}.getById(${t.pkPropertyName})

    suspend fun count${t.prefixPascal}() = ${t.daoPropertyName}.count()

    suspend fun insert${t.prefixPascal}(entity: ${t.entityClassName}) = ${t.daoPropertyName}.insert(entity)

    suspend fun insertAll${t.prefixPascal}(entities: List<${t.entityClassName}>) = ${t.daoPropertyName}.insertAll(entities)

    suspend fun update${t.prefixPascal}(entity: ${t.entityClassName}) = ${t.daoPropertyName}.update(entity)

    suspend fun delete${t.prefixPascal}(entity: ${t.entityClassName}) = ${t.daoPropertyName}.delete(entity)

    suspend fun delete${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType}) = ${t.daoPropertyName}.deleteById(${t.pkPropertyName})

    suspend fun deleteAll${t.prefixPascal}() = ${t.daoPropertyName}.deleteAll()
</#list>
}
