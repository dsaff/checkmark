package net.saff.checkmark.compose

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import net.saff.checkmark.Checkmark
import net.saff.junit.extract
import net.saff.junit.wrap
import org.robolectric.shadows.ShadowLog
import java.io.ByteArrayOutputStream
import java.io.PrintStream

data class ComposeRuleTestContext(val cr: ComposeContentTestRule) {
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
        cr.onRoot().printToLog("TAG")
      } catch (e: Throwable) {
        return "ERROR while computing compose tree string: ${e.message}"
      }
    } finally {
      ShadowLog.stream = oldStream
    }
    return baos.toString()
  }

  companion object {
    fun composeTest(fn: ComposeRuleTestContext.() -> Unit) =
      createComposeRule().wrap { cr ->
        val context = ComposeRuleTestContext(cr)
        Checkmark.checks {
          context.fn()
        }
      }

    fun <T> composeEval(fn: ComposeRuleTestContext.() -> T): T = extract {
      // SAFF: DUP above
      createComposeRule().wrap { cr ->
        val context = ComposeRuleTestContext(cr)
        Checkmark.checks {
          yield(context.fn())
        }
      }
    }
  }
}