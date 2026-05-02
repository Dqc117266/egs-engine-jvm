package com.dqc.egsengine.feature.scaffold.data.swagger

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

class SwaggerParser {
    private val logger = LoggerFactory.getLogger(SwaggerParser::class.java)

    fun parse(swaggerLocation: String): SwaggerSpec {
        val content = readContent(swaggerLocation)
        val root = JsonParser.parseString(content).asJsonObject

        val schemas = parseSchemas(root)
        val operations = parseOperations(root)

        logger.info("Parsed swagger: ${schemas.size} schemas, ${operations.size} operations")
        return SwaggerSpec(schemas = schemas, operations = operations)
    }

    private fun readContent(swaggerLocation: String): String {
        return when {
            swaggerLocation.startsWith("http://") || swaggerLocation.startsWith("https://") ->
                URI(swaggerLocation).toURL().readText()
            else -> File(swaggerLocation).readText()
        }
    }

    private fun parseSchemas(root: JsonObject): List<SwaggerSchema> {
        val schemasObj = root.getAsJsonObject("components")
            ?.getAsJsonObject("schemas")
            ?: return emptyList()

        return schemasObj.entrySet().mapNotNull { (name, schemaEl) ->
            val schema = schemaEl.asJsonObject
            val propertiesObj = schema.getAsJsonObject("properties") ?: JsonObject()
            val requiredSet = schema.getAsJsonArray("required")
                ?.map { it.asString }
                ?.toSet()
                ?: emptySet()

            val properties = propertiesObj.entrySet().map { (propName, propSchemaEl) ->
                val safeName = toSafePropertyName(propName)
                SwaggerProperty(
                    name = safeName,
                    originalName = propName,
                    type = resolveType(propSchemaEl),
                    required = requiredSet.contains(propName),
                )
            }
            SwaggerSchema(name = name, properties = properties)
        }
    }

    private fun parseOperations(root: JsonObject): List<SwaggerOperation> {
        val pathsObj = root.getAsJsonObject("paths") ?: return emptyList()
        val ops = mutableListOf<SwaggerOperation>()

        for ((path, pathEl) in pathsObj.entrySet()) {
            val pathObj = pathEl.asJsonObject
            val pathLevelParams = parseParameters(pathObj.getAsJsonArray("parameters"))

            for (method in HTTP_METHODS) {
                val opObj = pathObj.getAsJsonObject(method) ?: continue
                val opLevelParams = parseParameters(opObj.getAsJsonArray("parameters"))
                val allParams = (pathLevelParams + opLevelParams).distinctBy { "${it.location}:${it.name}" }
                val requestBody = parseRequestBody(opObj.getAsJsonObject("requestBody"))
                val responseBody = parseResponseBody(opObj.getAsJsonObject("responses"))
                val operationId = resolveOperationId(
                    method = method,
                    path = path,
                    rawOperationId = opObj.get("operationId")?.asString,
                    params = allParams,
                    responseBody = responseBody,
                )

                ops.add(
                    SwaggerOperation(
                        operationId = sanitizeMethodName(operationId),
                        method = method.uppercase(),
                        path = path.trimStart('/'),
                        params = allParams,
                        requestBody = requestBody,
                        responseBody = responseBody,
                    ),
                )
            }
        }
        return ops
    }

    private fun parseParameters(array: JsonArray?): List<SwaggerParameter> {
        if (array == null) return emptyList()
        return array.mapNotNull { el ->
            val obj = el.asJsonObject
            val name = obj.get("name")?.asString ?: return@mapNotNull null
            val location = obj.get("in")?.asString ?: "query"
            val required = obj.get("required")?.asBoolean ?: false
            val schema = obj.get("schema")
            SwaggerParameter(
                name = toSafePropertyName(name),
                originalName = name,
                location = location,
                required = required,
                type = resolveType(schema),
            )
        }
    }

    private fun parseRequestBody(requestBodyObj: JsonObject?): SwaggerType? {
        val contentObj = requestBodyObj?.getAsJsonObject("content") ?: return null
        val appJsonObj = contentObj.getAsJsonObject("application/json")
            ?: contentObj.entrySet().firstOrNull()?.value?.asJsonObject
            ?: return null
        return resolveType(appJsonObj.get("schema"))
    }

    private fun parseResponseBody(responsesObj: JsonObject?): SwaggerType? {
        if (responsesObj == null) return null
        val successResp = responsesObj.entrySet()
            .firstOrNull { it.key.startsWith("2") }
            ?.value
            ?.asJsonObject
            ?: return null
        val contentObj = successResp.getAsJsonObject("content") ?: return null
        val appJsonObj = contentObj.getAsJsonObject("application/json")
            ?: contentObj.entrySet().firstOrNull()?.value?.asJsonObject
            ?: return null
        return resolveType(appJsonObj.get("schema"))
    }

    private fun resolveType(schemaEl: JsonElement?): SwaggerType {
        if (schemaEl == null || schemaEl.isJsonNull) return SwaggerType.Unknown
        val obj = schemaEl.asJsonObject

        obj.get("\$ref")?.asString?.let { ref ->
            val modelName = ref.substringAfterLast("/")
            return SwaggerType.ModelRef(modelName)
        }

        when (obj.get("type")?.asString) {
            "string" -> return SwaggerType.Primitive(PrimitiveKind.STRING)
            "integer" -> {
                val fmt = obj.get("format")?.asString
                return if (fmt == "int64") {
                    SwaggerType.Primitive(PrimitiveKind.LONG)
                } else {
                    SwaggerType.Primitive(PrimitiveKind.INT)
                }
            }
            "number" -> return SwaggerType.Primitive(PrimitiveKind.DOUBLE)
            "boolean" -> return SwaggerType.Primitive(PrimitiveKind.BOOLEAN)
            "array" -> {
                val items = obj.get("items")
                return SwaggerType.ListType(resolveType(items))
            }
            "object" -> {
                val additional = obj.get("additionalProperties")
                return if (additional != null) {
                    SwaggerType.MapType(resolveType(additional))
                } else {
                    SwaggerType.MapType(SwaggerType.Unknown)
                }
            }
        }
        return SwaggerType.Unknown
    }

    private fun resolveOperationId(
        method: String,
        path: String,
        rawOperationId: String?,
        params: List<SwaggerParameter>,
        responseBody: SwaggerType?,
    ): String {
        val preservedOperationId = rawOperationId?.takeIf { shouldPreserveOperationId(it) }
        return preservedOperationId ?: deriveOperationIdFromPath(method, path, params, responseBody)
    }

    private fun shouldPreserveOperationId(rawOperationId: String): Boolean {
        if (rawOperationId.isBlank() || isWeakOperationId(rawOperationId)) {
            return false
        }
        if (rawOperationId.contains('_')) {
            return true
        }

        val tokens = operationTokens(rawOperationId)
        val firstActionIndex = tokens.indexOfFirst { it in ACTION_TOKENS }
        return when {
            firstActionIndex > 0 -> true
            firstActionIndex == -1 -> tokens.size >= 3
            else -> false
        }
    }

    private fun isWeakOperationId(rawOperationId: String): Boolean {
        val tokens = operationTokens(rawOperationId)
        if (tokens.isEmpty()) return true

        val compact = tokens.joinToString("")
        if (compact in WEAK_OPERATION_IDS) return true
        if (tokens.size == 1 && tokens.first() in ACTION_TOKENS) return true
        if (tokens.first() in ACTION_TOKENS && tokens.drop(1).all { it in WEAK_OPERATION_SUFFIXES }) return true

        return false
    }

    private fun deriveOperationIdFromPath(
        method: String,
        path: String,
        params: List<SwaggerParameter>,
        responseBody: SwaggerType?,
    ): String {
        val rawSegments = path.trim('/').split('/').filter { it.isNotBlank() }
        if (rawSegments.isEmpty()) return fallbackOperationId(method, path)

        val (basePrefixSegments, scopedSegments) = stripApiPrefix(rawSegments)
        val nonParamSegments = scopedSegments.filterNot(::isPathParameter)
        if (nonParamSegments.isEmpty()) return fallbackOperationId(method, path)

        val itemPath = scopedSegments.lastOrNull()?.let(::isPathParameter) == true && nonParamSegments.isNotEmpty()
        val tailSegment = when {
            itemPath -> null
            nonParamSegments.size > 1 -> nonParamSegments.last()
            else -> null
        }
        val resourceSegments = when {
            itemPath -> nonParamSegments
            tailSegment != null -> nonParamSegments.dropLast(1)
            else -> nonParamSegments
        }.ifEmpty { nonParamSegments.take(1) }
        if (resourceSegments.isEmpty()) return fallbackOperationId(method, path)

        val targetName = singularizeSegment(resourceSegments.last()).toSafePascal()
        val prefixName = buildPrefixName(
            basePrefixSegments = basePrefixSegments,
            contextSegments = resourceSegments.dropLast(1),
            targetName = targetName,
        )
        val actionName = deriveActionName(method, tailSegment, itemPath, targetName, params, responseBody)
        return "${prefixName}_${actionName}"
    }

    private fun stripApiPrefix(pathSegments: List<String>): Pair<List<String>, List<String>> {
        val first = pathSegments.firstOrNull()?.lowercase() ?: return emptyList<String>() to pathSegments
        return when (first) {
            "app-api" -> listOf("app") to pathSegments.drop(1)
            "admin-api", "api" -> emptyList<String>() to pathSegments.drop(1)
            else -> emptyList<String>() to pathSegments
        }
    }

    private fun buildPrefixName(
        basePrefixSegments: List<String>,
        contextSegments: List<String>,
        targetName: String,
    ): String {
        val normalizedBase = basePrefixSegments.map { singularizeSegment(it).toSafePascal() }
        val normalizedContext = contextSegments.map { singularizeSegment(it).toSafePascal() }
        val prefixSegments = if (normalizedContext.size >= 2 || (normalizedBase.isNotEmpty() && normalizedContext.isNotEmpty())) {
            normalizedBase + normalizedContext
        } else {
            emptyList()
        }
        return if (prefixSegments.isEmpty()) targetName else prefixSegments.joinToString("")
    }

    private fun deriveActionName(
        method: String,
        tailSegment: String?,
        itemPath: Boolean,
        targetName: String,
        params: List<SwaggerParameter>,
        responseBody: SwaggerType?,
    ): String {
        val upperMethod = method.uppercase()
        return when {
            itemPath -> when (upperMethod) {
                "GET" -> "Get$targetName"
                "PUT", "PATCH" -> "Update$targetName"
                "DELETE" -> "Delete$targetName"
                else -> "${defaultMethodVerb(upperMethod)}$targetName"
            }

            tailSegment == null -> when (upperMethod) {
                "GET" -> if (isPagedOperation(params, responseBody)) {
                    "Get${targetName}Page"
                } else {
                    "Get${targetName}List"
                }
                "POST" -> "Create$targetName"
                "PUT", "PATCH" -> "Update$targetName"
                "DELETE" -> "Delete$targetName"
                else -> "${defaultMethodVerb(upperMethod)}$targetName"
            }

            else -> when (tailSegment.lowercase()) {
                "count" -> "Count$targetName"
                "all" -> "GetAll$targetName"
                "list" -> "Get${targetName}List"
                "page" -> "Get${targetName}Page"
                else -> deriveCustomActionName(upperMethod, tailSegment, targetName)
            }
        }
    }

    private fun deriveCustomActionName(method: String, tailSegment: String, targetName: String): String {
        val tailWords = operationTokens(tailSegment)
        if (tailWords.isEmpty()) return "${defaultMethodVerb(method)}$targetName"

        val action = tailWords.first()
        val remainder = tailWords.drop(1).joinToString("") { it.toSafePascal() }
        return when {
            action == "delete" && remainder == "List" -> "Delete${targetName}List"
            action == "export" && remainder.isNotBlank() -> "Export${targetName}$remainder"
            action in CUSTOM_ACTION_TOKENS -> {
                val actionName = action.toSafePascal()
                if (remainder.isNotBlank()) "$actionName$remainder" else "$actionName$targetName"
            }

            else -> "${defaultMethodVerb(method)}${tailWords.joinToString("") { it.toSafePascal() }}"
        }
    }

    private fun isPagedOperation(params: List<SwaggerParameter>, responseBody: SwaggerType?): Boolean {
        val paramNames = params
            .filter { it.location.lowercase() != "header" }
            .map { it.originalName.lowercase() }
            .toSet()
        if (paramNames.any { it in PAGE_PARAM_NAMES }) return true

        return when (responseBody) {
            is SwaggerType.ModelRef -> responseBody.name.contains("PageResult", ignoreCase = true)
            else -> false
        }
    }

    private fun operationTokens(value: String): List<String> =
        value.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace(Regex("[^A-Za-z0-9]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .mapNotNull { token ->
                token.trimEnd { it.isDigit() }
                    .lowercase()
                    .takeIf { it.isNotBlank() }
            }

    private fun singularizeSegment(segment: String): String {
        val lower = segment.lowercase()
        return when {
            lower.endsWith("ies") && lower.length > 3 -> lower.dropLast(3) + "y"
            lower.endsWith("sses") || lower.endsWith("xes") || lower.endsWith("zes") ||
                lower.endsWith("ches") || lower.endsWith("shes") -> lower.dropLast(2)
            lower.endsWith("s") && !lower.endsWith("ss") && !lower.endsWith("us") -> lower.dropLast(1)
            else -> lower
        }
    }

    private fun isPathParameter(segment: String): Boolean =
        segment.startsWith("{") && segment.endsWith("}")

    private fun fallbackOperationId(method: String, path: String): String {
        val clean = path.split("/", "-", "{", "}")
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
        return method.lowercase() + clean
    }

    private fun defaultMethodVerb(method: String): String = when (method.uppercase()) {
        "GET" -> "Get"
        "POST" -> "Post"
        "PUT", "PATCH" -> "Update"
        "DELETE" -> "Delete"
        else -> method.lowercase().replaceFirstChar(Char::uppercase)
    }

    private fun sanitizeMethodName(name: String): String {
        val parts = name.split(Regex("[^A-Za-z0-9]")).filter { it.isNotBlank() }
        val camel = parts.mapIndexed { i, part ->
            if (i == 0) part.replaceFirstChar { c -> c.lowercase() }
            else part.replaceFirstChar { c -> c.uppercase() }
        }.joinToString("")
        val safe = camel.ifBlank { "autoGen" }
        return if (safe.firstOrNull()?.isDigit() == true) "_$safe" else safe
    }

    private fun toSafePropertyName(name: String): String {
        val camel = name.split("-", "_", ".")
            .filter { it.isNotBlank() }
            .mapIndexed { i, part ->
                if (i == 0) part.replaceFirstChar { it.lowercase() }
                else part.replaceFirstChar(Char::uppercase)
            }
            .joinToString("")
        return if (camel in KOTLIN_KEYWORDS) "${camel}Value" else camel
    }

    private companion object {
        val HTTP_METHODS = listOf("get", "post", "put", "delete", "patch")
        val KOTLIN_KEYWORDS = setOf(
            "class", "object", "when", "is", "in", "val", "var",
            "fun", "return", "package", "interface", "data",
        )
        val ACTION_TOKENS = setOf(
            "get", "list", "create", "update", "delete", "count", "add", "remove",
            "cancel", "check", "export", "sync", "refresh", "submit", "approve",
            "reject", "reset", "send", "verify", "upload", "download", "save",
            "post", "put", "patch",
        )
        val CUSTOM_ACTION_TOKENS = setOf(
            "cancel", "check", "export", "sync", "refresh", "submit", "approve",
            "reject", "reset", "send", "verify", "upload", "download",
        )
        val WEAK_OPERATION_IDS = setOf(
            "get", "list", "create", "update", "delete", "count", "all",
            "getbyid", "updatebyid", "deletebyid", "listpage", "listpath",
        )
        val WEAK_OPERATION_SUFFIXES = setOf("id", "path", "list", "page", "count", "all")
        val PAGE_PARAM_NAMES = setOf("page", "size", "pageno", "pagesize")
    }
}
