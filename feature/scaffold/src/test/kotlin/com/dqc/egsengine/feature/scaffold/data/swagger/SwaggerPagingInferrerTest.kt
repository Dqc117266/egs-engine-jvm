package com.dqc.egsengine.feature.scaffold.data.swagger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SwaggerPagingInferrerTest {
    @Test
    fun `PageResult prefixed schema yields paging with list item type`() {
        val userSchema =
            SwaggerSchema(
                "User",
                listOf(
                    SwaggerProperty("id", "id", SwaggerType.Primitive(PrimitiveKind.LONG), true),
                ),
            )
        val pageSchema =
            SwaggerSchema(
                "PageResultUser",
                listOf(
                    SwaggerProperty("list", "list", SwaggerType.ListType(SwaggerType.ModelRef("User")), false),
                    SwaggerProperty("total", "total", SwaggerType.Primitive(PrimitiveKind.LONG), false),
                    SwaggerProperty("page", "page", SwaggerType.Primitive(PrimitiveKind.INT), false),
                    SwaggerProperty("pageSize", "pageSize", SwaggerType.Primitive(PrimitiveKind.INT), false),
                    SwaggerProperty("totalPages", "totalPages", SwaggerType.Primitive(PrimitiveKind.INT), false),
                ),
            )
        val spec =
            SwaggerSpec(
                schemas = listOf(userSchema, pageSchema),
                operations =
                listOf(
                    SwaggerOperation(
                        operationId = "listUsers",
                        method = "GET",
                        path = "api/users",
                        params = emptyList(),
                        requestBody = null,
                        responseBody = SwaggerType.ModelRef("PageResultUser"),
                    ),
                ),
            )
        val enriched = SwaggerPagingInferrer().enrich(spec)
        val p = enriched.operations.single().paging
        assertNotNull(p)
        assertEquals(SwaggerType.ModelRef("User"), p!!.itemType)
        assertEquals("list", p.listPropertyName)
    }

    @Test
    fun `records field is accepted as list column`() {
        val item = SwaggerSchema("Item", emptyList())
        val pageSchema =
            SwaggerSchema(
                "PageDto",
                listOf(
                    SwaggerProperty("records", "records", SwaggerType.ListType(SwaggerType.ModelRef("Item")), false),
                    SwaggerProperty("total", "total", SwaggerType.Primitive(PrimitiveKind.LONG), false),
                    SwaggerProperty("page", "page", SwaggerType.Primitive(PrimitiveKind.INT), false),
                ),
            )
        val spec =
            SwaggerSpec(
                schemas = listOf(item, pageSchema),
                operations =
                listOf(
                    SwaggerOperation(
                        operationId = "list",
                        method = "GET",
                        path = "x",
                        params = emptyList(),
                        requestBody = null,
                        responseBody = SwaggerType.ModelRef("PageDto"),
                    ),
                ),
            )
        val p =
            SwaggerPagingInferrer()
                .enrich(spec)
                .operations
                .single()
                .paging
        assertNotNull(p)
        assertEquals("records", p!!.listPropertyName)
    }

    @Test
    fun `raw list response is not paging`() {
        val spec =
            SwaggerSpec(
                schemas = emptyList(),
                operations =
                listOf(
                    SwaggerOperation(
                        operationId = "all",
                        method = "GET",
                        path = "x",
                        params = emptyList(),
                        requestBody = null,
                        responseBody = SwaggerType.ListType(SwaggerType.ModelRef("User")),
                    ),
                ),
            )
        assertNull(
            SwaggerPagingInferrer()
                .enrich(spec)
                .operations
                .single()
                .paging,
        )
    }
}
