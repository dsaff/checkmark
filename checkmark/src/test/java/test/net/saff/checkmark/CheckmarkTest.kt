package test.net.saff.checkmark

import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.Checkmark.Companion.checkCompletes
import net.saff.checkmark.Checkmark.Companion.checks
import net.saff.checkmark.thrown
import net.saff.prettyprint.showWhitespace
import org.junit.Test

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