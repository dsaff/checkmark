package test.net.saff.checkmark

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.saff.befuzz.chooseString
import net.saff.befuzz.exploreTreeFates
import net.saff.befuzz.theory
import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.Checkmark.Companion.fail
import net.saff.checkmark.thrown
import net.saff.checkmark.useJson
import net.saff.prettyprint.showWhitespace
import org.junit.Test
import java.io.File
import java.util.regex.Matcher

class JsonMessageAssemblerTest {
    @Test
    fun jsonOutputWithMark() {
        val message = useJson { thrown { "A".check { it == mark("B") } }!!.message!! }
        val element = message.storedJsonElement()
        element.jsonObject["actual"].check { it?.jsonPrimitive?.content == "A" }
        element.jsonObject["marked"].check { it?.jsonPrimitive?.content == "B" }
    }

    @Test
    fun jsonOutputDontRepeatMark() {
        val element = useJson {
            thrown {
                val note = "B"
                "A".check { it == mark(note) }
            }!!.message!!
        }.storedJsonElement()

        element.jsonObject["actual"].check { it?.jsonPrimitive?.content == "A" }
        element.check { it.jsonObject["note"]?.jsonPrimitive?.content == "B" }
        element.jsonObject.check { !it.contains("marked") }
    }

    private fun String.jsonFileFromMessage() = jsonMatcher().check {
        mark(this@jsonFileFromMessage)
        it.find()
    }.group(1) ?: fail(this)

    @Test
    fun jsonOutputWithList() {
        val message =
            useJson { thrown { listOf("A").check { it == mark(listOf("B", "C")) } }!!.message!! }

        message.check { it.contains("[A]") }
        message.check { !it.contains("B") }
        val element = message.storedJsonElement()
        val o = element.jsonObject
        o["actual"]?.asList().check { it == listOf("A".primitive) }
        o["marked"].check { obj ->
            obj?.asList() == listOf("B", "C").map { it.primitive }
        }
    }

    private fun JsonElement.asList() = jsonArray.toList()

    @Test
    fun jsonOutputWithMap() {
        val message = useJson {
            thrown { listOf("A").check { it == mark(mapOf("B" to 2, "C" to 3)) } }!!.message!!
        }

        val o = message.storedJsonElement().jsonObject
        o["actual"]?.asList().check { it == listOf("A".primitive) }
        o["marked"]?.jsonObject!!.check { obj ->
            obj["B"] == "2".primitive && obj["C"] == "3".primitive
        }
    }

    @Test
    fun jsonOutputWithOnlyActualButIsList() {
        val message =
            useJson { thrown { listOf("A").check { it == listOf("B", "C") } }!!.message!! }

        val element = message.storedJsonElement()
        element.jsonObject["actual"]?.asList().check { it == listOf("A".primitive) }
        element.jsonObject["marked"].check { it == null }
    }

    private val String.primitive get() = JsonPrimitive(this)

    private fun String.storedJsonElement(): JsonElement {
        val jsonFile = jsonFileFromMessage().check { it.startsWith("/tmp") }
        return Json.parseToJsonElement(File(jsonFile).readText())
    }

    @Test
    fun jsonOutputWithOnlyActual() {
        val expect = "Failed assertion: A".trimMargin().showWhitespace()
        useJson { thrown { "A".check { it == "B" } } }!!.message!!.showWhitespace()
            .check { it == expect }
    }

    @Test
    fun jsonOutputWithMarkTheory() {
        theory(exploreTreeFates(1)) {
            val expectedActual = chooseString("actual?")
            val expectedMarked = chooseString("marked?")
            if (expectedActual != expectedMarked) {
                val element = useJson {
                    thrown { expectedActual.check { it == mark(expectedMarked) } }!!.message!!
                }.storedJsonElement()
                element.jsonObject["actual"]
                    .check { it?.jsonPrimitive?.content == expectedActual }
                element.jsonObject["expectedMarked"]
                    .check { it?.jsonPrimitive?.content == expectedMarked }
            }
        }
    }

    private fun String.jsonMatcher(): Matcher =
        "\\[more: file://(.*\\.json)]".toPattern().matcher(this)
}