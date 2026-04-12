/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${databasePackageName}

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
<#list tables as t>
import ${t.entityPackageName}.${t.entityClassName}
import ${t.daoPackageName}.${t.daoClassName}
</#list>

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ${moduleDatabaseName}Constructor : RoomDatabaseConstructor<${moduleDatabaseName}> {
    override fun initialize(): ${moduleDatabaseName}
}

@Database(
    entities = [
<#list tables as t>
        ${t.entityClassName}::class<#if t?has_next>,</#if>
</#list>
    ],
    version = 1,
    exportSchema = true,
    autoMigrations = [],
)
@ConstructedBy(${moduleDatabaseName}Constructor::class)
abstract class ${moduleDatabaseName} : RoomDatabase() {
<#list tables as t>

    abstract fun ${t.daoPropertyName}(): ${t.daoClassName}
</#list>
}
