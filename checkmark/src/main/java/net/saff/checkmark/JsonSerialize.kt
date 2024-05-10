package net.saff.checkmark

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Any?.jsonSerialize(): JsonElement {
    return when (this) {
        is List<*> -> JsonArray(map { it.jsonSerialize() })

        is Map<*, *> ->
            JsonObject(mapValues { it.value.jsonSerialize() }.mapKeys { it.key.toString() })

        else -> JsonPrimitive(toString())
    }
}