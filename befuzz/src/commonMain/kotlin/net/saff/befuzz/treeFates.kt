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

package net.saff.befuzz

fun exploreTreeFates(maxBits: Int): Fates {
  fun newFate(bitSource: Int, nextOptions: MutableList<Int>): Fate = object : Fate {
    var mask = 1

    override fun scryBit(): Int {
      val returnThis = (bitSource and mask).countOneBits()
      if (returnThis == 0 && mask > bitSource && bitSource.countOneBits() < maxBits) {
        val newOption = bitSource or mask
        if (!nextOptions.contains(newOption)) {
          nextOptions.add(newOption)
        }
      }
      mask = mask.shl(1)
      return returnThis
    }

    override fun freshCopy(): Fate {
      return newFate(bitSource, nextOptions)
    }

    override fun hint(): String {
      return "treeFate($bitSource)"
    }
  }

  return Fates {
    sequence {
      val nextOptions = mutableListOf(0)

      while (nextOptions.isNotEmpty()) {
        yield(newFate(nextOptions.removeAt(0), nextOptions))
      }
    }
  }
}
