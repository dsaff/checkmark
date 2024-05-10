package test.net.saff.checkmark

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.saff.befuzz.chooseString
import net.saff.befuzz.exploreTreeFates
import net.saff.befuzz.theory
import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.Checkmark.Companion.checkCompletes
import net.saff.checkmark.Checkmark.Companion.checks
import net.saff.checkmark.Checkmark.Companion.fail
import net.saff.checkmark.Checkmark.Companion.useJson
import net.saff.checkmark.thrown
import net.saff.prettyprint.showWhitespace
import org.junit.Test
import java.io.File
import java.util.regex.Matcher

class CheckmarkTest {
    @Test
    fun dontPrintFunctions() {
        val fn = { "abc" }
        thrown { checks { "def".check { it == fn() } } }!!.message!!.check { !it.contains("->") }
    }

    @Test
    fun checkIncludesCaptures() {
        val n = "Apple sauce"
        thrown { "Pear soup".check { it == n } }!!.message.check { it!!.contains("Apple") }
    }

    @Test
    fun outputWithMark() {
        val expect = """
        |Failed assertion: 
        |- actual: A
        |- marked: B
    """.trimMargin().showWhitespace()
        thrown { "A".check { it == mark("B") } }!!.message!!.showWhitespace().check {
            it == expect
        }
    }

    @Test
    fun outputWithMarkFn() {
        val expect = """
        |Failed assertion: 
        |- actual: A
        |- marked: B
    """.trimMargin().showWhitespace()
        thrown {
            "A".check {
                var toMark = "C"
                mark { toMark }
                toMark = "B"
                it == "B"
            }
        }!!.message!!.showWhitespace().check { it == expect }
    }

    @Test
    fun onlyActual() {
        val expect = "Failed assertion: A".showWhitespace()
        thrown { "A".check { it == "B" } }!!.message!!.showWhitespace().check { it == expect }
    }

    @Test
    fun jsonOutputWithMark() {
        val message = useJson { thrown { "A".check { it == mark("B") } }!!.message!! }
        val jsonFile = message.jsonFileFromMessage()
        jsonFile.check { it.startsWith("/tmp") }
        val element = Json.parseToJsonElement(File(jsonFile).readText())
        // SAFF: DUP?
        element.jsonObject["actual"].check { it?.jsonPrimitive?.content == "A" }
        element.jsonObject["marked"].check { it?.jsonPrimitive?.content == "B" }
    }

    @Test
    fun jsonOutputDontRepeatMark() {
        val message = useJson {
            thrown {
                val note = "B"
                "A".check { it == mark(note) }
            }!!.message!!
        }

        val jsonFile = message.jsonFileFromMessage()
        jsonFile.check { it.startsWith("/tmp") }
        val element = Json.parseToJsonElement(File(jsonFile).readText())
        // SAFF: DUP?
        element.jsonObject["actual"].check { it?.jsonPrimitive?.content == "A" }
        element.jsonObject["note"].check { it?.jsonPrimitive?.content == "B" }
        element.jsonObject.check { !it.contains("marked") }
    }

    private fun String.jsonFileFromMessage(): String {
        return jsonMatcher().check {
            // SAFF: this outputs both marked and message.  Only one is needed
            mark(this@jsonFileFromMessage)
            it.find()
        }.group(1) ?: fail(this)
    }

    @Test
    fun jsonOutputWithList() {
        // SAFF: DUP above?
        val message =
            useJson { thrown { listOf("A").check { it == mark(listOf("B", "C")) } }!!.message!! }

        message.check { it.contains("[A]") }
        message.check { !it.contains("B") }
        val jsonFile = message.jsonFileFromMessage()
        jsonFile.check { it.startsWith("/tmp") }
        val element = Json.parseToJsonElement(File(jsonFile).readText())
        // SAFF: DUP?
        val actualList = element.jsonObject["actual"]?.jsonArray?.toList()
        actualList.check { it == listOf(JsonPrimitive("A")) }
        element.jsonObject["marked"].check { obj ->
            obj?.jsonArray?.toList() == listOf("B", "C").map { JsonPrimitive(it) }
        }
    }

    @Test
    fun jsonOutputWithMap() {
        // SAFF: DUP above?
        val message = useJson {
            thrown { listOf("A").check { it == mark(mapOf("B" to 2, "C" to 3)) } }!!.message!!
        }

        val jsonFile = message.jsonFileFromMessage()
        jsonFile.check { it.startsWith("/tmp") }
        val element = Json.parseToJsonElement(File(jsonFile).readText())
        // SAFF: DUP?
        val actualList = element.jsonObject["actual"]?.jsonArray?.toList()
        actualList.check { it == listOf(JsonPrimitive("A")) }
        element.jsonObject["marked"]?.jsonObject!!.check { obj ->
            obj["B"] == JsonPrimitive("2") && obj["C"] == JsonPrimitive("3")
        }
    }

    // SAFF: should we include first line before json link?
    @Test
    fun jsonOutputWithOnlyActualButIsList() {
        // SAFF: DUP above?
        val message =
            useJson { thrown { listOf("A").check { it == listOf("B", "C") } }!!.message!! }

        val jsonFile = message.jsonFileFromMessage()
        jsonFile.check { it.startsWith("/tmp") }
        val element = Json.parseToJsonElement(File(jsonFile).readText())
        // SAFF: DUP?
        val actualList = element.jsonObject["actual"]?.jsonArray?.toList()
        actualList.check { it == listOf(JsonPrimitive("A")) }
        element.jsonObject["marked"].check { it == null }
    }

    @Test
    fun jsonOutputWithOnlyActual() {
        // SAFF: but json when the value is more interesting
        val expect = "Failed assertion: A".trimMargin().showWhitespace()
        useJson { thrown { "A".check { it == "B" } } }!!.message!!.showWhitespace()
            .check { it == expect }
    }

    @Test
    fun jsonOutputWithMarkTheory() {
        // SAFF: DUP above
        theory(exploreTreeFates(1)) {
            val expectedActual = chooseString("actual?")
            val expectedMarked = chooseString("marked?")
            if (expectedActual != expectedMarked) {
                // SAFF: clean up
                val message =
                    useJson { thrown { expectedActual.check { it == mark(expectedMarked) } }!!.message!! }

                val jsonFile = message.jsonFileFromMessage()
                val element = Json.parseToJsonElement(File(jsonFile).readText())
                // SAFF: DUP?
                element.jsonObject["actual"]
                    .check { it?.jsonPrimitive?.content == expectedActual }
                element.jsonObject["expectedMarked"]
                    .check { it?.jsonPrimitive?.content == expectedMarked }
            }
        }
    }

    // SAFF: DUP callers?
    private fun String.jsonMatcher(): Matcher =
        "\\[more: file://(.*\\.json)]".toPattern().matcher(this)

    @Test
    fun meldStackTrace() {
        var rootStackTrace: List<StackTraceElement>? = null
        thrown {
            checks {
                val re = RuntimeException("flubber")
                rootStackTrace = re.stackTrace.toList()
                throw re
            }
        }!!.stackTrace.toList().check { trace -> trace[0] == rootStackTrace!![0] }
    }
    // SAFF: long

    @Test
    fun checkCompletesWorks() {
        thrown {
            checkCompletes {
                mark("abc")
                throw RuntimeException("foo")
            }
        }!!.message!!.check { it.contains("abc") }
    }

    @Test
    fun onlyReturnMessageOnce() {
        val n = "Apple\nsauce"
        thrown { "Pear soup".check { it == n } }!!.let { error ->
            error.message!!.check { it.contains("Apple") }
            error.message!!.check { it == "[duplicate message suppressed]" }
        }
    }

    @Test
    fun onlySuppressMultipleLines() {
        thrown { "Pear soup".check { it == "Apple sauce" } }!!.let { error ->
            error.message!!.check { it.contains("Pear") }
            error.message!!.check { it.contains("Pear") }
        }
    }
}