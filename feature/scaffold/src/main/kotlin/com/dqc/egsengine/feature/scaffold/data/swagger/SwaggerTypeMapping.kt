package com.dqc.egsengine.feature.scaffold.data.swagger

/**
 * Shared type resolution and expression mapping logic for Swagger codegen.
 * Platform-specific contexts delegate to these pure functions.
 */
internal object SwaggerTypeMapping {
    fun resolveType(
        type: SwaggerType,
        forDomain: Boolean,
        dataModelName: (String) -> String,
        domainModelName: (String) -> String,
    ): String = when (type) {
        is SwaggerType.Primitive -> when (type.kind) {
            PrimitiveKind.STRING -> "String"
            PrimitiveKind.INT -> "Int"
            PrimitiveKind.LONG -> "Long"
            PrimitiveKind.DOUBLE -> "Double"
            PrimitiveKind.BOOLEAN -> "Boolean"
        }

        is SwaggerType.ModelRef ->
            if (forDomain) domainModelName(type.name) else dataModelName(type.name)

        is SwaggerType.ListType ->
            "List<${resolveType(type.elementType, forDomain, dataModelName, domainModelName)}>"

        is SwaggerType.MapType ->
            "Map<String, ${resolveType(type.valueType, forDomain, dataModelName, domainModelName)}>"

        SwaggerType.Unknown -> "JsonElement"
    }

    fun importsForType(
        type: SwaggerType,
        forDomain: Boolean,
        currentPackage: String?,
        dataModelPackage: String,
        domainModelPackage: String,
        dataModelName: (String) -> String,
        domainModelName: (String) -> String,
    ): Set<String> = when (type) {
        is SwaggerType.Primitive -> emptySet()
        is SwaggerType.ModelRef -> {
            val pkg = if (forDomain) domainModelPackage else dataModelPackage
            val simple = if (forDomain) domainModelName(type.name) else dataModelName(type.name)
            if (currentPackage != null && pkg == currentPackage) emptySet() else setOf("$pkg.$simple")
        }
        is SwaggerType.ListType -> importsForType(type.elementType, forDomain, currentPackage, dataModelPackage, domainModelPackage, dataModelName, domainModelName)
        is SwaggerType.MapType -> importsForType(type.valueType, forDomain, currentPackage, dataModelPackage, domainModelPackage, dataModelName, domainModelName)
        SwaggerType.Unknown -> setOf("kotlinx.serialization.json.JsonElement")
    }

    fun requiresToDomainImport(type: SwaggerType?): Boolean {
        val t = type ?: return false
        return when (t) {
            is SwaggerType.ModelRef -> true
            is SwaggerType.ListType -> requiresToDomainImport(t.elementType)
            is SwaggerType.MapType -> requiresToDomainImport(t.valueType)
            is SwaggerType.Primitive, SwaggerType.Unknown -> false
        }
    }

    fun mapExpressionNonNull(
        type: SwaggerType,
        sourceExpr: String,
        method: String,
    ): String = when (type) {
        is SwaggerType.Primitive, SwaggerType.Unknown -> sourceExpr
        is SwaggerType.ModelRef -> "$sourceExpr.$method()"
        is SwaggerType.ListType -> "$sourceExpr.map { ${mapExpressionNonNull(type.elementType, "it", method)} }"
        is SwaggerType.MapType -> "$sourceExpr.mapValues { (_, value) -> ${mapExpressionNonNull(type.valueType, "value", method)} }"
    }

    fun mapExpression(
        type: SwaggerType,
        sourceExpr: String,
        nullableContainer: Boolean,
        method: String,
    ): String {
        val mappedExpr = mapExpressionNonNull(type, sourceExpr, method)
        if (!nullableContainer) return mappedExpr
        return when (type) {
            is SwaggerType.Primitive, SwaggerType.Unknown -> sourceExpr
            is SwaggerType.ModelRef -> "$sourceExpr?.$method()"
            is SwaggerType.ListType -> "$sourceExpr?.map { ${mapExpressionNonNull(type.elementType, "it", method)} }"
            is SwaggerType.MapType -> "$sourceExpr?.mapValues { (_, value) -> ${mapExpressionNonNull(type.valueType, "value", method)} }"
        }
    }
}
