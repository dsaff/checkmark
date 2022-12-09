package net.saff.checkmark.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import net.saff.checkmark.Checkmark
import net.saff.junit.extract
import net.saff.junit.wrap

data class ComposeRuleTestContext<T : ComposeTestRule>(val cr: T) {
    val log = mutableListOf<String>()

    override fun toString(): String {
        return "log: $log\n${composeTreeString()}"
    }

    private fun composeTreeString(): String {
        return try {
            cr.onAllNodes(isRoot()).printToString(Int.MAX_VALUE)
        } catch (e: Throwable) {
            "ERROR while computing compose tree string: ${e.message}"
        }
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