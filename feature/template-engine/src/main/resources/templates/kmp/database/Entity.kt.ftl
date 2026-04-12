/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ${entityPackageName}

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "${table.tableName}")
data class ${entityClassName}(
<#list columns as col>
<#if col.isPrimaryKey>
    @PrimaryKey<#if col.autoGenerate>(autoGenerate = true)</#if>
<#else>
    @ColumnInfo(name = "${col.name}")
</#if>
    val ${col.kotlinPropertyName}: ${col.kotlinType}${col.nullableMark}
<#sep>,

</#sep>
</#list>
)
