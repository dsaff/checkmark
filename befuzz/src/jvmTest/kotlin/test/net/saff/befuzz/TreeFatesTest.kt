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

import net.saff.checkmark.Checkmark.Companion.check
import net.saff.befuzz.Adventure
import net.saff.befuzz.FateFromInt
import net.saff.befuzz.choose
import net.saff.befuzz.chooseBoolean
import net.saff.befuzz.exploreTreeFates
import net.saff.befuzz.scryPath
import net.saff.befuzz.theory
import net.saff.checkmark.thrown
import org.junit.Test

class TreeFatesTest {
  @Test
  fun treeFateFirstAttempt() {
    buildList {
      theory(exploreTreeFates(maxBits = 3)) {
        add(chooseStringOfOnes())
      }
    }.check { it == listOf("", "1", "11", "111") }
  }

  @Test
  fun fateNumberIncludedInFailure() {
    thrown {
      theory(exploreTreeFates(maxBits = 7)) {
        chooseStringOfOnes().check { it.length < 6 }
      }
    }!!.message.check { it!!.contains("63") }
  }

  @Test
  fun fateNumberIncludedInFailure5bits() {
    thrown {
      theory(exploreTreeFates(maxBits = 7)) {
        chooseStringOfOnes().check { it.length < 5 }
      }
    }!!.message!!.check { it.contains("31") }.check { !it.contains("63") }
  }

  @Test
  fun canSeedWithNumber() {
    buildList {
      theory(FateFromInt(63)) {
        add(chooseStringOfOnes())
      }
    }.check { it == listOf("111111") }
  }

  @Test
  fun fateFromIntHint() {
    FateFromInt(62).hint().check { it == "intFate(62)" }
    FateFromInt(37).hint().check { it == "intFate(37)" }
  }

  @Test
  fun treeFateTwoBits() {
    buildList {
      theory(exploreTreeFates(maxBits = 2)) {
        add(chooseStringOfOnes())
      }
    }.check { it == listOf("", "1", "11") }
  }

  private fun Adventure.chooseStringOfOnes(): String {
    return buildString {
      while (chooseBoolean("Another?")) {
        append("1")
      }
    }
  }

  @Test
  fun treeFateChooseFromTwoStrings() {
    buildList {
      theory(exploreTreeFates(maxBits = 3)) {
        add(buildString {
          while (chooseBoolean("Another?")) {
            append(
              choose("Which string") {
                scryPath("A", "B")
              }
            )
          }
        })
      }
    }.check { it == listOf("", "A", "B", "AA", "BA", "AB", "AAA") }
  }
}