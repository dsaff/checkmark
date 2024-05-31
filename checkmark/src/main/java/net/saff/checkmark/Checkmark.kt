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

interface Structured {
    fun toStructure(): Any
}

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
                println("big message retrieved: $bigMessageRetrieved")
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
            fun debugOutput() = allDebugOutput {
                add("actual" to this@check)
                addAll(eval.extractClosureFields())
                addAll(cm.markReports())
            }

            val result = try {
                cm.eval(this)
            } catch (t: Throwable) {
                fail("ERROR: ${debugOutput()}", t)
            }
            if (!result) {
                fail("Failed assertion: ${debugOutput()}")
            }
            return this
        }

        fun checks(fn: Checkmark.() -> Unit) {
            val cm = Checkmark()
            try {
                cm.fn()
            } catch (e: Throwable) {
                fail(
                    allDebugOutput {
                        addAll(fn.extractClosureFields())
                        addAll(cm.markReports())
                    }, e
                )
            }
        }

        private fun Any.extractClosureFields() = buildList {
            this@extractClosureFields::class.java.declaredFields.forEach { field ->
                // Not sure why this is needed.
                // https://github.com/dsaff/checkmark/issues/1
                if (field.name != "INSTANCE") {
                    field.isAccessible = true
                    val gotten = field.get(this@extractClosureFields)
                    if (gotten !is Function<*>) {
                        add(field.name.removePrefix("\$") to gotten.structured())
                    }
                }
            }
        }

        private fun Checkmark.markReports() = marks.map { "marked" to it() }

        private fun allDebugOutput(fn: MutableList<Pair<String, Any?>>.() -> Unit): String {
            return assembleMessage(buildList(fn).distinctBy { it.second })
        }

        private fun assembleMessage(reports: List<Pair<String, Any?>>): String {
            reports.singleOrNull()?.second?.let { single ->
                if (single !is Collection<*>) {
                    return single.cleanString()
                }
            }
            return messageAssembler.assembleComplexMessage(reports)
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

        fun <T> useMessageAssembler(assembler: MessageAssembler, fn: () -> T): T {
            val previous = messageAssembler
            messageAssembler = assembler
            try {
                return fn()
            } finally {
                messageAssembler = previous
            }
        }

        private var messageAssembler: MessageAssembler = StringMessageAssembler
        var suppressDuplicateMessages = true
    }
}

private fun Any?.structured(): Any? {
    return if (this is Structured) {
        toStructure()
    } else {
        this
    }
}

interface MessageAssembler {
    fun assembleComplexMessage(reports: List<Pair<String, Any?>>): String
}

data object StringMessageAssembler : MessageAssembler {
    override fun assembleComplexMessage(reports: List<Pair<String, Any?>>): String {
        return reports.cleanPairsAsLines()
    }
}

fun Any?.cleanString() = orElse("null").toString().forCleanDisplay()

fun List<Pair<String, Any?>>.cleanPairsAsLines() =
    joinToString(separator = "") { "\n- ${it.first}: ${it.second.toString().forCleanDisplay()}" }

private fun String.forCleanDisplay(): String {
    return if (!contains("\n")) {
        this
    } else {
        val margin = "  |"
        "\n$margin${replace("\n", "\n$margin")}"
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