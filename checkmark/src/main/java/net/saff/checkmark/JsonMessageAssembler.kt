package net.saff.checkmark

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data object JsonMessageAssembler : MessageAssembler {
    override fun assembleMessage(reports: List<Pair<String, Any?>>): String {
        // SAFF: DUP other?
        reports.singleOrNull()?.second?.let { single ->
            if (single.jsonSerialize() is JsonPrimitive) {
                return single.cleanString()
            }
        }

        val cleanPairsForDisplay = reports.cleanPairsAsLines()
        val jsonObject = reports.toMap().jsonSerialize()
        val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val dateString = format.format(Date())
        val file = File("/tmp/compare_$dateString.json")
        // Create a uniquely named file with a timestamp in the filename in /tmp

        file.writeText(jsonObject.toString())

        val firstLine = cleanPairsForDisplay.lines().first { it.isNotBlank() }
        return "$firstLine [more: file://${file}]"
    }
}

// SAFF: move
fun Any?.jsonSerialize(): JsonElement {
    return when (this) {
        is List<*> -> JsonArray(map { it.jsonSerialize() })

        is Map<*, *> ->
            JsonObject(mapValues { it.value.jsonSerialize() }.mapKeys { it.key.toString() })

        else -> JsonPrimitive(toString())
    }
}

fun <T> useJson(fn: () -> T) = Checkmark.useMessageAssembler(JsonMessageAssembler, fn)