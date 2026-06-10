package com.dqc.egsengine.feature.scaffold.data.swagger

data class SwaggerSpec(
    val schemas: List<SwaggerSchema>,
    val operations: List<SwaggerOperation>,
)

data class SwaggerSchema(
    val name: String,
    val properties: List<SwaggerProperty>,
)

data class SwaggerProperty(
    val name: String,
    val originalName: String,
    val type: SwaggerType,
    val required: Boolean,
)

data class SwaggerOperation(
    val operationId: String,
    val method: String,
    val path: String,
    val params: List<SwaggerParameter>,
    val requestBody: SwaggerType?,
    val responseBody: SwaggerType?,
    /** Set by Swagger paging inference for KMP codegen when the contract is a paged payload. */
    val paging: PagingInfo? = null,
)

/**
 * Inferred pagination: domain [itemType] is the element type of the list inside [pageResultClass].
 */
data class PagingInfo(
    val itemType: SwaggerType,
    /** Kotlin property name on the API model for the list field (e.g. `list`, `records`). */
    val listPropertyName: String,
    val pageParam: String = "page",
    val sizeParam: String = "pageSize",
)

data class SwaggerParameter(
    val name: String,
    val originalName: String,
    val location: String,
    val required: Boolean,
    val type: SwaggerType,
)

sealed class SwaggerType {
    data class Primitive(
        val kind: PrimitiveKind,
    ) : SwaggerType()

    data class ModelRef(
        val name: String,
    ) : SwaggerType()

    data class ListType(
        val elementType: SwaggerType,
    ) : SwaggerType()

    data class MapType(
        val valueType: SwaggerType,
    ) : SwaggerType()

    data object Unknown : SwaggerType()
}

enum class PrimitiveKind {
    STRING,
    INT,
    LONG,
    DOUBLE,
    BOOLEAN,
}
