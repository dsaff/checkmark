package test.net.saff.checkmark.compose

import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.compose.ComposeRuleTestContext.Companion.composeTest
import net.saff.checkmark.thrown
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeRuleTestContextTest {
    @Test
    fun foo() = composeTest {
        cr.setContent {
            Text(text = "Here!")
        }

        cr.onNodeWithText("Here!").assertIsDisplayed()
    }

    @Test
    fun failureMessage() {
        thrown {
            composeTest {
                cr.setContent {
                    Text(text = "Here!")
                }

                cr.onNodeWithText("There!").assertIsDisplayed()
            }
        }!!.message!!.check { it.contains("[Here!]") }
    }
}