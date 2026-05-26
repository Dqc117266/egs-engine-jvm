/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${daoPackageName}

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ${entityPackageName}.${entityClassName}

@Dao
interface ${daoClassName} {

    @Query("SELECT COUNT(*) FROM ${table.tableName}")
    suspend fun count(): Long

    @Query("SELECT * FROM ${table.tableName} ORDER BY ${orderByColumn} ASC")
    suspend fun getAll(): List<${entityClassName}>

    @Query("SELECT * FROM ${table.tableName} WHERE ${pkColumnName} = :${pkPropertyName}")
    suspend fun getById(${pkPropertyName}: ${pkKotlinType}): ${entityClassName}?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ${entityClassName})

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<${entityClassName}>)

    @Update
    suspend fun update(entity: ${entityClassName})

    @Delete
    suspend fun delete(entity: ${entityClassName})

    @Query("DELETE FROM ${table.tableName} WHERE ${pkColumnName} = :${pkPropertyName}")
    suspend fun deleteById(${pkPropertyName}: ${pkKotlinType})

    @Query("DELETE FROM ${table.tableName}")
    suspend fun deleteAll()
}
