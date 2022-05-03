package test.net.saff.checkmark

import net.saff.checkmark.Checkmark.Companion.check
import net.saff.checkmark.showWhitespace
import net.saff.checkmark.thrown
import org.junit.Test

class CheckmarkTest {
  @Test
  fun checkIncludesCaptures() {
    val n = "Apple sauce"
    thrown { "Pear soup".check { it == n } }!!.message.check { it!!.contains("Apple") }
  }

  // SAFF: are failures printed twice on purpose?
  @Test
  fun outputWithMark() {
    // SAFF: no "this" if there's only this
    val expect = """
        |Failed assertion: 
        |- actual: A
        |- marked: B
    """.trimMargin().showWhitespace()
    thrown { "A".check { it == mark("B") } }!!.message!!.showWhitespace().check {
      it == expect
    }
  }
}