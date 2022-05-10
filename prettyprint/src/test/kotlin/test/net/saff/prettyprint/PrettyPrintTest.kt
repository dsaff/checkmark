package test.net.saff.prettyprint

import net.saff.prettyprint.showWhitespace
import org.junit.Assert
import org.junit.Test

class PrettyPrintTest {
  @Test
  fun showWhiteSpace() {
    Assert.assertEquals("a_b", "a b".showWhitespace())
  }
}