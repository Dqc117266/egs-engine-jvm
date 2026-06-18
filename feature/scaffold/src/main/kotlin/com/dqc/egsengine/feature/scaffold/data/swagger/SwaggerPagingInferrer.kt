/*
 * Copyright 2026 Mifos Initiative
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.dqc.egsengine.feature.scaffold.data.swagger

import com.dqc.egsengine.feature.scaffold.data.generator.common.PagePagingDetector
import com.dqc.egsengine.feature.scaffold.domain.model.UseCaseParam

/**
 * Infers paged REST operations from OpenAPI shapes (response-first).
 * Run after KMP wrapper unwrapping ([KmpSwaggerCodeGenerator.adjustSpecForKmp]) so
 * [SwaggerOperation.responseBody] matches generated types.
 */
class SwaggerPagingInferrer {
    fun enrich(spec: SwaggerSpec): SwaggerSpec = spec.copy(operations = spec.operations.map { op -> op.copy(paging = inferPaging(op, spec)) })

    private fun inferPaging(
        op: SwaggerOperation,
        spec: SwaggerSpec,
    ): PagingInfo? {
        val resp = op.responseBody ?: return null

        // Veto: raw array/list responses are not paged envelopes.
        if (resp is SwaggerType.ListType) return null

        val schemaByName = spec.schemas.associateBy { it.name }
        val schema = resolveSchema(resp, schemaByName) ?: return null

        if (!looksLikePageSchema(schema)) return null

        val (listProp, itemType) = extractListField(schema) ?: return null
        val pageNames = detectPageParams(op)

        return PagingInfo(
            itemType = itemType,
            listPropertyName = listProp,
            pageParam = pageNames.page,
            sizeParam = pageNames.pageSize,
        )
    }

    private fun resolveSchema(
        type: SwaggerType,
        schemaByName: Map<String, SwaggerSchema>,
    ): SwaggerSchema? = when (type) {
        is SwaggerType.ModelRef -> schemaByName[type.name]
        else -> null
    }

    /**
     * Strong signal A: schema name contains `PageResult` (springdoc generic expansion).
     * Strong signal B: shape — list-like array + total + at least one page/size field.
     */
    private fun looksLikePageSchema(schema: SwaggerSchema): Boolean {
        val nameHit = schema.name.contains("PageResult", ignoreCase = true)
        val props = schema.properties.map { it.originalName.lowercase() }.toSet()

        val hasList = LIST_KEYS.any { it in props }
        val hasTotal = props.contains("total") || props.contains("totalcount")
        if (!hasList || !hasTotal) return nameHit && hasList && hasTotal

        val hasPageSignal =
            props.any {
                it in setOf("page", "pageno", "pageindex", "pagesize", "size", "limit", "totalpages")
            }
        return nameHit || hasPageSignal
    }

    private fun extractListField(schema: SwaggerSchema): Pair<String, SwaggerType>? {
        for (key in LIST_KEYS) {
            val prop =
                schema.properties.find { it.originalName.equals(key, ignoreCase = true) }
                    ?: continue
            when (val t = prop.type) {
                is SwaggerType.ListType -> return prop.name to t.elementType
                else -> continue
            }
        }
        return null
    }

    private fun detectPageParams(op: SwaggerOperation): PagePagingDetector.PageParamNames {
        val names = op.params.map { UseCaseParam(it.name, "Int") }
        return PagePagingDetector.detectPageParams(names)
    }

    private companion object {
        private val LIST_KEYS = listOf("list", "records", "content", "items")
    }
}
