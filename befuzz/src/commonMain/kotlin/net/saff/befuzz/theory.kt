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

import kotlinx.serialization.Serializable

fun <T> Evidence.goOnAdventure(fate: Fate, fn: Adventure.() -> T): T {
  val adventure = Adventure(fate)
  try {
    val result = adventure.fn()
    logSuccessfulAdventure(adventure)
    return result
  } catch (t: Throwable) {
    throw GoodDataException(adventure.logAsString(), t)
  }
}

fun theory(fates: Fates, fn: Adventure.() -> Unit): Evidence {
  return Evidence().apply {
    fates.allFates().forEach {
      goOnAdventure(it, fn)
    }
  }
}

fun Fate.asFates() = Fates { sequenceOf(this@asFates) }

fun theory(fate: Fate, fn: Adventure.() -> Unit) = theory(fate.asFates(), fn)

fun interface Fates {
  fun allFates(): Sequence<Fate>
}

class Evidence {
  private val adventures = mutableListOf<Adventure>()

  fun logSuccessfulAdventure(adventure: Adventure) {
    adventures.add(adventure)
  }

  fun anyAdventure(fn: Adventure.() -> Boolean) = adventures.any { it.fn() }
  override fun toString() = adventures.joinToString("\n")
}

expect class GoodDataException(adventureLog: String, cause: Throwable) : RuntimeException

@Serializable
data class AdventureLog(val choices: List<Pair<String, String>>, val answer: String)

class Adventure(private val fate: Fate) {
  private val choices = mutableListOf<Pair<String, String>>()

  private val stepLog = mutableListOf<String>()

  override fun toString() =
    """|ADVENTURE(${fate.hint()})
       |${choices.joinToString("\n") { "  ${it.first} => ${it.second}" }}
       |== LOG ==
       |${stepLog.joinToString("\n")}
       |""".trimMargin()

  fun logAsString() = toString()

  fun logStep(step: String) {
    stepLog.add(step)
  }

  fun extractLog(answer: String) = AdventureLog(choices, answer)

  fun <T> chooseLabeled(question: String, fn: Fate.() -> Pair<String, T>): T {
    val answer = fate.fn()
    choices.add(question to answer.first)
    return answer.second
  }

  fun sawChoice(question: String, answer: Any?) =
    choices.any { it.first == question && it.second == answer.toString() }

  fun sawChoice(question: String) = choices.any { it.first == question }

  fun sawChoicesInOrder(vararg expectedChoices: Pair<String, Any?>): Boolean {
    val remainingChoices = expectedChoices.toMutableList()
    choices.forEach {
      val nextExpected = remainingChoices[0]
      if (it.first == nextExpected.first && it.second == nextExpected.second.toString()) {
        remainingChoices.removeAt(0)
        if (remainingChoices.isEmpty()) {
          return true
        }
      }
    }
    return false
  }

  fun <T> T.assume(fn: (T) -> Boolean): T {
    if (!fn(this)) {
      throw AssumptionViolatedException(toString())
    }
    return this
  }
}

class AssumptionViolatedException(message: String) : RuntimeException(message)

fun <T> Adventure.choose(question: String, fn: Fate.() -> T) =
  chooseLabeled(question) { fn().run { toString() to this } }

fun <T> Adventure.chooseFrom(question: String, vararg options: T) =
  chooseLabeled(question) { scryPath(*options).let { it.toString() to it } }

fun <T> Adventure.chooseFromNested(
  question: String,
  vararg options: Pair<String, Adventure.() -> T>
) = chooseLabeled(question) {
  val option = scryPath(*options)
  val generate = option.second
  option.first to this@chooseFromNested.generate()
}

fun Adventure.chooseIntLessThan(question: String, n: Int) = choose(question) {
  scryIntLessThan(n)
}

fun Adventure.chooseSmallNaturalNumber(question: String) = choose(question) {
  var i = 0
  while (scryBit() == 1) {
    i++
  }
  i
}

fun Adventure.chooseBoolean(question: String) = choose(question) {
  scryIntLessThan(2) == 1
}

interface Fate {
  fun scryBit(): Int
  fun freshCopy(): Fate
  fun hint(): String
}

fun Fate.scryIntLessThan(n: Int): Int {
  if (n == 1) {
    return 0
  }

  val lowOrderBit = scryBit()
  return scryIntLessThan((n + 1 - lowOrderBit) / 2) * 2 + lowOrderBit
}

fun fatesTo(i: Int) = Fates { (0 until i + 1).map { FateFromInt(it) }.asSequence() }

class FateFromInt(private val byteSource: Int) : Fate {
  private var remainingByteSource = byteSource

  override fun scryBit(): Int {
    val bit = remainingByteSource and 1
    remainingByteSource = remainingByteSource.shr(1)
    return bit
  }

  override fun freshCopy() = FateFromInt(byteSource)

  override fun hint() = "intFate($byteSource)"
}

fun <T> Fate.scryPath(vararg choices: T) = choices[scryIntLessThan(choices.size)]
fun Adventure.chooseString(question: String) = choose(question) { scryString() }
fun Fate.scryString() = scryPath("Satsuki", "Mei", "Totoro", "")

fun <T> Adventure.chooseStepAndExecute(vararg steps: Pair<String, () -> T>): T {
  return chooseLabeled("Step") {
    steps[scryIntLessThan(steps.size)]
  }()
}

fun <T> converge(fates: Fates, vararg comparees: T, fn: Adventure.(T) -> String): Evidence {
  return Evidence().apply {
    fates.allFates().forEach { fate ->
      comparees.map { goOnAdventure(fate.freshCopy()) { fn(it) to logAsString() } }.let {
        if (it.toMap().size != 1) {
          throw RuntimeException(it.toString())
        }
      }
    }
  }
}