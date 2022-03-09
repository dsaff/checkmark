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

class Checkmark {
    class Failure(s: String, e: Throwable? = null) : java.lang.RuntimeException(s, e)

    private val marks = mutableListOf<() -> String>()

    fun mark(note: String): String {
        marks.add { note }
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
                fail("ERROR: ${allDebugOutput(cm)}", t)
            }
            if (!result) {
                fail("Failed assertion: ${allDebugOutput(cm)}")
            }
            return this
        }

        fun checks(fn: () -> Unit) {
            try {
                fn()
            } catch (e: Throwable) {
                val data =
                    extractClosureFields(fn).joinToString("") { it.run { "\n$first: [[$second]]" } }
                fail(data, e)
            }
        }

        private fun extractClosureFields(closure: Any) = buildList {
            closure::class.java.declaredFields.forEach { field ->
                field.isAccessible = true
                add(field.name to field.get(closure))
            }
        }

        private fun <T> T.allDebugOutput(cm: Checkmark) = "<<$this>>${cm.marks()}"

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