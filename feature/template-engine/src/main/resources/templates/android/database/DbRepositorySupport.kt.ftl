/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${repositorySupportPackageName}

import ${databasePackageName}.${moduleDatabaseName}DataSource
import ${repositoryPackageName}.${dbRepositoryName}
<#list entityImports as imp>
import ${imp}
</#list>

/**
 * Generated support for [${dbRepositoryName}]: delegates to [${moduleDatabaseName}DataSource].
 */
internal class ${dbRepositorySupportName}(
    private val dbDataSource: ${moduleDatabaseName}DataSource,
) : ${dbRepositoryName} {
<#list tables as t>

    override suspend fun get${t.prefixPascal}All() = dbDataSource.get${t.prefixPascal}All()

    override suspend fun get${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType}) =
        dbDataSource.get${t.prefixPascal}ById(${t.pkPropertyName})

    override suspend fun count${t.prefixPascal}() = dbDataSource.count${t.prefixPascal}()

    override suspend fun insert${t.prefixPascal}(entity: ${t.entityClassName}) =
        dbDataSource.insert${t.prefixPascal}(entity)

    override suspend fun insertAll${t.prefixPascal}(entities: List<${t.entityClassName}>) =
        dbDataSource.insertAll${t.prefixPascal}(entities)

    override suspend fun update${t.prefixPascal}(entity: ${t.entityClassName}) =
        dbDataSource.update${t.prefixPascal}(entity)

    override suspend fun delete${t.prefixPascal}(entity: ${t.entityClassName}) =
        dbDataSource.delete${t.prefixPascal}(entity)

    override suspend fun delete${t.prefixPascal}ById(${t.pkPropertyName}: ${t.pkKotlinType}) =
        dbDataSource.delete${t.prefixPascal}ById(${t.pkPropertyName})

    override suspend fun deleteAll${t.prefixPascal}() = dbDataSource.deleteAll${t.prefixPascal}()
</#list>
}
