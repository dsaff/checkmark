package test.net.saff.junit

import net.saff.checkmark.Checkmark.Companion.check
import net.saff.junit.wrap
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

class JunitKtxTest {
    @Test fun testWrap() {
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
}