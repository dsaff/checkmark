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