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

                val operationId = opObj.get("operationId")?.asString
                    ?: fallbackOperationId(method, path)
                val requestBody = parseRequestBody(opObj.getAsJsonObject("requestBody"))
                val responseBody = parseResponseBody(opObj.getAsJsonObject("responses"))

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

    private fun fallbackOperationId(method: String, path: String): String {
        val clean = path.split("/", "-", "{", "}")
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
        return method.lowercase() + clean
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
    }
}
