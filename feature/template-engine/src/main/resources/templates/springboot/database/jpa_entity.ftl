${warningGenerated}
package ${generatePackage}.data.datasource.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
<#if useJpaAuditingBase>
import ${sharedRoot}.infrastructure.persistence.BaseEntity
</#if>

@Entity
@Table(name = "${tableSqlName}")
class ${entityPascal}Entity<#if useJpaAuditingBase> : BaseEntity()</#if> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var ${pkProp}: ${pkKotlinType} = <#if pkKotlinType == "Long">0L<#else>0</#if>

<#list entityBodyColumns as col>
    <#if col.maxLength??>
    @Column(nullable = ${col.nullable?c}, length = ${col.maxLength?c})
    <#else>
    @Column(nullable = ${col.nullable?c})
    </#if>
    <#if col.kotlinType == "String" && !col.nullable>
    lateinit var ${col.kotlinName}: String
    <#elseif col.kotlinType == "String">
    var ${col.kotlinName}: String? = null
    <#elseif col.kotlinName == "status" && col.kotlinType == "Int">
    var ${col.kotlinName}: Int = 1
    <#elseif col.nullable>
    var ${col.kotlinName}: ${col.kotlinType}? = null
    <#else>
    var ${col.kotlinName}: ${col.kotlinType} = <#if col.kotlinType == "Long">0L<#elseif col.kotlinType == "Boolean">false<#else>0</#if>
    </#if>

</#list>
}
