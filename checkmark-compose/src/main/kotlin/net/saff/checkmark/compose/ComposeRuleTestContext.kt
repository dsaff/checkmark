package net.saff.checkmark.compose

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.saff.checkmark.Checkmark
import net.saff.checkmark.compose.ComposeRuleTestContext.Companion.clickVisible
import net.saff.junit.extract
import net.saff.junit.ring
import net.saff.junit.wrap
import kotlin.coroutines.resume

data class ComposeRuleTestContext<T : ComposeTestRule>(val cr: T) {
    val logs = mutableListOf<String>()

    override fun toString(): String {
        return "log: ${logs.map { "\n    $it" }}\n${composeTreeString()}"
    }

    private fun composeTreeString(): String {
        return try {
            cr.onAllNodes(isRoot()).printToString(Int.MAX_VALUE)
        } catch (e: Throwable) {
            "ERROR while computing compose tree string: ${e.message}"
        }
    }

    fun click(text: String) = cr.click(text)

    companion object {
        suspend fun composeRing() = createComposeRule().composeRing()

        @OptIn(ExperimentalCoroutinesApi::class)
        fun composeTest(fn: suspend ComposeRuleTestContext<ComposeContentTestRule>.() -> Unit) =
            composeEval { runTest { fn() } }

        fun <T> composeEval(fn: ComposeRuleTestContext<ComposeContentTestRule>.() -> T) =
            createComposeRule().composeEval(fn)

        suspend fun <U : ComposeTestRule> U.composeRing() =
            ring { this@composeRing.composeEval { resume(this@composeRing) } }

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

        fun SemanticsNodeInteraction.clickVisible() = assertIsDisplayed().performClick()
    }
}

fun ComposeTestRule.click(text: String) = onNodeWithText(text).clickVisible()