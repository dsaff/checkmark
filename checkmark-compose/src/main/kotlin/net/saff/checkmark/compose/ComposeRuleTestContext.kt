package net.saff.checkmark.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import net.saff.checkmark.Checkmark
import net.saff.junit.extract
import net.saff.junit.wrap
import org.robolectric.shadows.ShadowLog
import java.io.ByteArrayOutputStream
import java.io.PrintStream

data class ComposeRuleTestContext<T : ComposeTestRule>(val cr: T) {
    val log = mutableListOf<String>()

    override fun toString(): String {
        return "log: $log\n${composeTreeString()}"
    }

    fun composeTreeString(): String {
        val oldStream = ShadowLog.stream
        val baos = ByteArrayOutputStream()
        try {
            ShadowLog.stream = PrintStream(baos)
            try {
                cr.onAllNodes(isRoot()).printToLog("TAG", Int.MAX_VALUE)
            } catch (e: Throwable) {
                return "ERROR while computing compose tree string: ${e.message}"
            }
        } finally {
            ShadowLog.stream = oldStream
        }
        return baos.toString()
    }

    fun click(text: String) {
        cr.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    companion object {
        fun composeTest(fn: ComposeRuleTestContext<ComposeContentTestRule>.() -> Unit) =
            composeEval(fn)

        fun <T> composeEval(fn: ComposeRuleTestContext<ComposeContentTestRule>.() -> T): T {
            val rule = createComposeRule()
            return rule.composeEval(fn)
        }

        fun <T, U : ComposeTestRule> U.composeEval(
            fn: ComposeRuleTestContext<U>.() -> T
        ): T {
            return extract {
                wrap { cr ->
                    val context = ComposeRuleTestContext(cr)
                    Checkmark.checks {
                        yield(context.fn())
                    }
                }
            }
        }
    }
}