package test.net.saff.checkmark

import kotlinx.serialization.json.Json
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

val checkmarkMarker = java.lang.AssertionError()

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

    // SAFF: DUP?
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
        }!!.message!!.showWhitespace().check {
            it == expect
        }
    }

    @Test
    fun onlyActual() {
        // SAFF: DUP above?
        val expect = "Failed assertion: A".trimMargin().showWhitespace()
        thrown { "A".check { it == "B" } }!!.message!!.showWhitespace().check { it == expect }
    }

    @Test
    fun jsonOutputWithMark() {
        val message = useJson { thrown { "A".check { it == mark("B") } }!!.message!! }

        val matcher = "\\[more: (.*\\.json)]".toPattern().matcher(message)
        val jsonFile = matcher.check {
            // SAFF: this outputs both marked and message.  Only one is needed
            mark(message)
            it.find()
        }.group(1) ?: fail(message)
        jsonFile.check { it.startsWith("/tmp") }
        val element = Json.parseToJsonElement(File(jsonFile).readText())
        // SAFF: DUP?
        element.jsonObject.get("actual").check { it?.jsonPrimitive?.content == "A" }
        element.jsonObject.get("marked").check { it?.jsonPrimitive?.content == "B" }
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
                val message =
                    useJson { thrown { expectedActual.check { it == mark(expectedMarked) } }!!.message!! }

                val matcher = "\\[more: (.*\\.json)]".toPattern().matcher(message)
                val jsonFile = matcher.check {
                    // SAFF: this outputs both marked and message.  Only one is needed
                    mark(message)
                    it.find()
                }.group(1) ?: fail(message)
                val element = Json.parseToJsonElement(File(jsonFile).readText())
                // SAFF: DUP?
                element.jsonObject.get("actual")
                    .check { it?.jsonPrimitive?.content == expectedActual }
                element.jsonObject.get("marked")
                    .check { it?.jsonPrimitive?.content == expectedMarked }
            }
        }
    }

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
        val n = "Apple sauce"
        thrown { "Pear soup".check { it == n } }!!.let { error ->
            error.message!!.check { it.contains("Apple") }
            error.message!!.check { it == "[duplicate message suppressed]" }
        }
    }
}