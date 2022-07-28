/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.net.saff.befuzz

import java.util.LinkedList
import net.saff.checkmark.Checkmark.Companion.check
import net.saff.befuzz.chooseBoolean
import net.saff.befuzz.chooseString
import net.saff.befuzz.converge
import net.saff.befuzz.exploreTreeFates
import net.saff.befuzz.fatesTo
import net.saff.befuzz.theory
import net.saff.checkmark.thrown
import org.junit.Test

class TheoryTest {
  @Test
  fun fatesToZero() {
    buildList {
      theory(fatesTo(0)) {
        add(chooseBoolean(""))
      }
    }.check { it == listOf(false) }
  }

  @Test
  fun meldStackTrace() {
    var rootStackTrace: Array<StackTraceElement>? = null
    thrown {
      theory(fatesTo(1)) {
        val re = RuntimeException("flubber")
        rootStackTrace = re.stackTrace
        throw re
      }
    }!!.stackTrace.toList().check { trace -> trace[0] == rootStackTrace!![0] }
  }

  @Test
  fun convergeSometimesPasses() {
    val aList = ArrayList<String>()
    val lList = LinkedList<String>()
    converge(exploreTreeFates(maxBits = 1), aList, lList) {
      while (chooseBoolean("Another?")) {
        it.add(chooseString("What to add?"))
      }
      it.toString()
    }
  }

  @Test
  fun convergeReturnsEvidence() {
    val aList = ArrayList<String>()
    val lList = LinkedList<String>()
    converge(exploreTreeFates(maxBits = 1), aList, lList) {
      while (chooseBoolean("Another?")) {
        it.add(chooseString("What to add?"))
      }
      it.toString()
    }.check { it.anyAdventure { sawChoice("Another?", "false") } }
  }

  @Test
  fun singleParamSawChoice() {
    val aList = ArrayList<String>()
    val lList = LinkedList<String>()
    converge(exploreTreeFates(maxBits = 1), aList, lList) {
      while (chooseBoolean("Another?")) {
        it.add(chooseString("What to add?"))
      }
      it.toString()
    }.check { it.anyAdventure { sawChoice("Another?") } }
  }

  @Test
  fun convergeFromInt() {
    val aList = ArrayList<String>()
    val lList = LinkedList<String>()
    converge(fatesTo(3), aList, lList) {
      while (chooseBoolean("Another?")) {
        it.add(chooseString("What to add?"))
      }
      it.toString()
    }
  }

  @Test
  fun convergeSometimesFails() {
    val getFive = { 5 }
    val getSix = { 6 }
    thrown {
      converge(exploreTreeFates(0), getFive, getSix) {
        it().toString()
      }
    }.check { it != null }
  }


  @Test
  fun convergeExploresEachFateOnce() {
    val getFive = { 5 }
    val getFiveAgain = { 5 }
    val chosen = mutableListOf<Boolean>()
    converge(exploreTreeFates(1), getFive, getFiveAgain) {
      chosen.add(chooseBoolean("Don't use"))
      it().toString()
    }
    chosen.check { it == listOf(false, false, true, true) }
  }

  @Test
  fun fateHint() {
    exploreTreeFates(1).allFates().first().hint().check { it == "treeFate(0)" }
  }
}