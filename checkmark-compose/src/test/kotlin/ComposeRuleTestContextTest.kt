import ComposeRuleTestContext.Companion.composeTest
import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
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
}