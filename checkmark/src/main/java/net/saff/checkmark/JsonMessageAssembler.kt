package net.saff.checkmark

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data object JsonMessageAssembler : MessageAssembler {
    private val json = Json { prettyPrint = true }
    override fun assembleComplexMessage(reports: List<Pair<String, Any?>>): String {
        val cleanPairsForDisplay = reports.cleanPairsAsLines()
        val jsonObject = reports.toMap().jsonSerialize()
        val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val dateString = format.format(Date())
        val file = File("/tmp/compare_$dateString.json")
        file.writeText(json.encodeToString(jsonObject))

        val firstLine = cleanPairsForDisplay.lines().first { it.isNotBlank() }
        return "$firstLine [more: file://${file}]"
    }
}

fun Any?.jsonSerialize(): JsonElement {
    return when (this) {
        is List<*> -> JsonArray(map { it.jsonSerialize() })

        is Map<*, *> ->
            JsonObject(mapValues { it.value.jsonSerialize() }.mapKeys { it.key.toString() })

        else -> JsonPrimitive(toString())
    }
}

fun <T> useJson(fn: () -> T) = Checkmark.useMessageAssembler(JsonMessageAssembler, fn)