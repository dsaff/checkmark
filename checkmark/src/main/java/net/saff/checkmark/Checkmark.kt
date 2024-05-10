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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.saff.prettyprint.cleanPairsForDisplay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Checkmark {
    class Failure(s: String, e: Throwable? = null) :
        AssertionError(e?.message?.let { m -> m + "\n" + s } ?: s, e) {
        override fun getStackTrace(): Array<StackTraceElement> {
            val causeHere = cause
            return if (causeHere != null) {
                causeHere.stackTrace
            } else {
                super.getStackTrace()
            }
        }

        // IntelliJ + Gradle tends to print error messages twice, messing up the output.
        // Therefore, only return the real error message once (yes, this could backfire, but we'll
        // handle that when it comes)
        private var bigMessageRetrieved = false
        override val message: String?
            get() {
                System.out.println("big message retrieved: $bigMessageRetrieved")
                if (!bigMessageRetrieved || !suppressDuplicateMessages) {
                    val message = super.message
                    if (message?.contains("\n") == true) {
                        bigMessageRetrieved = true
                    }
                    return message
                }
                return "[duplicate message suppressed]"
            }

        override fun getLocalizedMessage(): String? {
            return super.message ?: super.getLocalizedMessage()
        }
    }

    private val marks = mutableListOf<() -> Any?>()

    fun <T> mark(note: T): T {
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
                if (field.name != "INSTANCE") {
                    field.isAccessible = true
                    val gotten = field.get(closure)
                    if (!(gotten is Function<*>)) {
                        add(field.name.removePrefix("\$") to gotten)
                    }
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

            // SAFF: but we may still want JSON if second has structure
            val single = reports.singleOrNull()
            if (single != null) {
                if (useJson) {
                    if (single.second.jsonSerialize() is JsonPrimitive) {
                        return single.second.orElse("null").toString().forCleanDisplay()
                    }
                } else {
                    // SAFF: DUP above
                    return single.second.orElse("null").toString().forCleanDisplay()
                }
            }

            if (useJson) {
                // SAFF: lists
                // SAFF: maps
                // SAFF: not all values are going to be strings, are they?
                val contentMap =
                    reports.associate {
                        val value = it.second
                        it.first to value.jsonSerialize()
                    }
                val jsonObject = JsonObject(contentMap)
                val format = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                val dateString = format.format(Date())
                val file = File("/tmp/compare_$dateString.json")
                // Create a uniquely named file with a timestamp in the filename in /tmp

                file.writeText(jsonObject.toString())
                // SAFF: include _something_?
                return "[more: $file]"
            }

            // SAFF: match all of this with above
            return cleanPairsForDisplay(reports)
        }

        private fun cleanPairsForDisplay(reports: List<Pair<String, Any?>>) =
            reports.joinToString(separator = "") {
                "\n- ${it.first}: ${it.second.toString().forCleanDisplay()}"
            }

        private fun String.forCleanDisplay(): String {
            return if (!contains("\n")) {
                this
            } else {
                val margin = "  |"
                "\n$margin${replace("\n", "\n$margin")}"
            }
        }

        fun <T> T.checkCompletes(eval: Checkmark.(T) -> Unit): T {
            val cm = Checkmark()
            try {
                cm.eval(this)
            } catch (e: Throwable) {
                throw Exception("Failed: <<$this>>${cm.marks()}", e)
            }
            return this
        }

        // SAFF: choose an indentation style
        fun <T> useJson(fn: () -> T): T {
            useJson = true
            try {
                return fn()
            } finally {
                useJson = false
            }
        }

        private var useJson = false
        var suppressDuplicateMessages = true
    }
}

private fun <T> T?.orElse(sub: T) = this ?: sub

fun thrown(fn: () -> Any?): Throwable? {
    try {
        fn()
    } catch (t: Throwable) {
        return t
    }
    return null
}

private fun Any?.jsonSerialize(): JsonElement {
    return if (this is List<*>) {
        JsonArray(this.map { it.jsonSerialize() })
    } else {
        JsonPrimitive(toString())
    }
}