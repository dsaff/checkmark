package net.saff.prettyprint


fun String.showWhitespace() = replace(" ", "_").replace("\n", "\\\n")

fun List<Pair<String, Any?>>.cleanPairsForDisplay() =
  joinToString("\n") { "- ${it.first}: ${it.second.toString().forCleanDisplay(false)}" }

private fun String.forCleanDisplay(realString: Boolean = true): String {
  return if (!contains("\n")) {
    this
  } else {
    val margin = if (realString) { "  |" } else { "  " }
    "\n$margin${replace("\n", "\n$margin")}"
  }
}

fun Any?.prettyPrint(): String {
  when (this) {
    is Map<*, *> -> {
      return entries.map { it.key.toString() to it.value.prettyPrint() }.cleanPairsForDisplay()
    }
    is Collection<*> -> {
      return mapIndexed { i, value -> i.toString() to value.prettyPrint() }.cleanPairsForDisplay()
    }
    else -> {
      return toString()
    }
  }
}