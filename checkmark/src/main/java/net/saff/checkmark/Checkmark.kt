/*
Copyright 2022 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package net.saff.checkmark

import net.saff.prettyprint.cleanPairsForDisplay

class Checkmark {
  class Failure(s: String, e: Throwable? = null) :
    java.lang.RuntimeException(e?.message?.let { m -> m + "\n" + s } ?: s, e) {
    override fun getStackTrace(): Array<StackTraceElement> {
      val causeHere = cause
      return if (causeHere != null) {
        causeHere.stackTrace
      } else {
        super.getStackTrace()
      }
    }
  }

  private val marks = mutableListOf<() -> String>()

  fun <T> mark(note: T): T {
    marks.add { note.toString() }
    return note
  }

  fun mark(fn: () -> String) {
    marks.add(fn)
  }

  private fun marks() = marks.joinToString { "\n  mark: ${it()}" }

  companion object {
    fun fail(s: String, e: Throwable? = null): Nothing = throw Failure(s, e)

    fun <T> T.check(eval: Checkmark.(T) -> Boolean): T {
      val cm = Checkmark()
      val result = try {
        cm.eval(this)
      } catch (t: Throwable) {
        fail("ERROR: ${allDebugOutput(this, cm, eval)}", t)
      }
      if (!result) {
        fail("Failed assertion: ${allDebugOutput(this, cm, eval)}")
      }
      return this
    }

    fun checks(fn: () -> Unit) {
      try {
        fn()
      } catch (e: Throwable) {
        fail(extractClosureFields(fn).cleanPairsForDisplay(), e)
      }
    }

    private fun extractClosureFields(closure: Any) = buildList {
      closure::class.java.declaredFields.forEach { field ->
        // Not sure why this is needed.
        // https://github.com/dsaff/checkmark/issues/1
        // SAFF: separate out prettyPrint
        // SAFF: don't print functions?
        if (field.name != "INSTANCE") {
          field.isAccessible = true
          add(field.name.removePrefix("\$") to field.get(closure))
        }
      }
    }

    private fun <T> allDebugOutput(
      receiver: T, cm: Checkmark, eval: Checkmark.(T) -> Boolean
    ): String {
      val reports = buildList {
        add("actual" to receiver)
        addAll(extractClosureFields(eval))
        addAll(cm.marks.map { "marked" to it() })
      }
      return if (reports.size == 1) {
        reports[0].second.toString().forCleanDisplay()
      } else {
        cleanPairsForDisplay(reports)
      }
    }

    private fun cleanPairsForDisplay(reports: List<Pair<String, Any?>>) =
      reports.joinToString("") { "\n- ${it.first}: ${it.second.toString().forCleanDisplay()}" }

    private fun String.forCleanDisplay(): String {
      return if (!contains("\n")) {
        this
      } else {
        val margin = "  |"
        "\n$margin${replace("\n", "\n$margin")}"
      }
    }

    // SAFF: remove for checks?
    fun <T> T.checkCompletes(eval: Checkmark.(T) -> Unit): T {
      val cm = Checkmark()
      try {
        cm.eval(this)
      } catch (e: Throwable) {
        throw Exception("Failed: <<$this>>${cm.marks()}", e)
      }
      return this
    }
  }
}

fun thrown(fn: () -> Any?): Throwable? {
  try {
    fn()
  } catch (t: Throwable) {
    return t
  }
  return null
}