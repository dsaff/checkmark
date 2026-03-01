package test.net.saff.junit

import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.thrown
import net.saff.junit.ring
import net.saff.junit.rings
import net.saff.junit.wrap
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import kotlin.coroutines.resume

class JunitKtxTest {
    @Test
    fun testWrap() {
        buildList {
            val rule = TestRule { base, _ ->
                object : Statement() {
                    override fun evaluate() {
                        add("before")
                        base.evaluate()
                        add("after")
                    }
                }
            }
            rule.wrap { add("during") }
        }.check { it == listOf("before", "during", "after") }
    }

    @Test
    fun ringsEverythingGood() {
        var status = "starting"
        rings {
            status.check { it == "starting" }
            val three = ring {
                status = "in ring"
                resume(3)
                status = "after resume"
            }
            three.check { it == 3 }
            status.check { it == "in ring" }
        }
        status.check { it == "after resume" }
    }

    @Test
    fun ringsThereIsAFailure() {
        var status = "starting"
        thrown {
            rings {
                status.check { it == "starting" }
                val three = ring {
                    status = "in ring"
                    try {
                        resume(3)
                    } finally {
                        status = "after resume"
                    }
                }
                three.check { it == 4 }
                status.check { it == "in ring" }
            }
        }.check { it!!.message!!.contains("assertion: 3") }
        status.check { it == "after resume" }
    }

    @Test
    fun ringsRethrow() {
        var status = "starting"
        thrown {
            rings {
                status.check { it == "starting" }
                val three = ring {
                    status = "in ring"
                    try {
                        resume(3)
                    } catch (t: Throwable) {
                        throw RuntimeException("something different!")
                    } finally {
                        status = "after resume"
                    }
                }
                three.check { it == 4 }
                status.check { it == "in ring" }
            }
        }.check { it!!.message!!.contains("different") }
        status.check { it == "after resume" }
    }

    @Test
    fun ringsRethrowWithCause() {
        val e = thrown {
            rings {
                val three = ring {
                    try {
                        resume(3)
                    } catch (t: Throwable) {
                        throw RuntimeException("something different!", t)
                    }
                }
                three.check { it == 4 }
            }
        }
        e.check { it!!.message!!.contains("different") }
    }

    // Does this matter for rings?  Maybe, if we're trying to add data
    // an AssertionError.  https://github.com/gradle/gradle/pull/26399/files
    // https://github.com/gradle/gradle/issues/26299
    //
    // Do I want to do something about this?
}