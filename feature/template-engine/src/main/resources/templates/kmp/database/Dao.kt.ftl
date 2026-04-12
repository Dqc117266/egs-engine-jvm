/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${daoPackageName}

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ${entityPackageName}.${entityClassName}

@Dao
interface ${daoClassName} {

    @Query("SELECT COUNT(*) FROM ${table.tableName}")
    suspend fun count(): Long

    @Query("SELECT * FROM ${table.tableName} ORDER BY ${orderByColumn} ASC")
    suspend fun getAll(): List<${entityClassName}>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<${entityClassName}>)
}
