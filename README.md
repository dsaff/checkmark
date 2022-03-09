# Checkmark

The primary purpose of this repository is to demonstrate a
"Minimum Viable Assertion API", focused on the single method `check()`.

`check` does the two main things that one wants when making a test assertion:

1. Causes the test to fail if a property is not true.
1. Reports data _that is not obvious from reading the test code_ to help
   determine why the property is false.

(There are also other, more experimental functions that may stand the test
of time, or find other places to live.)

Example usage of `check`:

```
@Test
fun basicEquality() {
  val ls = listOf("A", "B", "C")

  ls.size.check { it == 3 }

  ls.check { ! it.contains("D") }

  // Fails with "Failed assertion: <<3>>"
  ls.size.check { it == 2 }

  // Fails with "Failed assertion: <<[A, B, C]>>"
  ls.check { it.size == 2 }    
}

```

Checkmark makes it easy to also report computed values other than the receiver
of `check`, using `mark`:

```
@Test
fun testMark() {
  val ls1 = listOf("A", "B", "C")
  val ls2 = listOf("D", "E", "F")

  // Fails with:
  // """
  // Failed assertion: <<[A, B, C]>>
  //  mark: D
  // """
  ls1.check { it.contains(mark(ls2[0])) }
}
```

On JVM, checkmark can take advantage of closures and reflection tricks to
extract local variable names of captured values in error messages, using
`marked`:

```
@Test
fun testMarked() {
  val ls1 = listOf("A", "B", "C")
  val ls2 = listOf("D", "E", "F")

  // Fails with:
  // """
  // $ls1: [[[A, B, C]]]
  // $ls2: [[[D, E, F]]]
  // """
  marked {
    ls1.check { it.intersect(ls2).isNotEmpty() }
  }
}
```

Things that checkmark does _not_ do:

1. Unlike JUnit and most assertion libraries, checkmark does not yet contain any
   code to help to extract differences between two structured values that are
   asserted to be equal.  So it will not yet help you eyeball the difference
   between two long strings, or point out the one changed value in a large
   array.  This functionality may be eventually added, but it may also make
   sense to eventually create or reuse a separate library that could be used by
   checkmark or any other library that needs to create helpful diff output.

1. Unlike some assertion practices and frameworks, checkmark does not, and
   will never, try to help the developer avoid looking at the stacktrace
   and test code.  There will never be an "assertIsNotEmpty" method that
   throws an exception message "Expected foo to not be empty, but it was
   empty".  This is for multiple reasons:
   1. Doing this well for all things a developer might want to assert requires
      an order of magnitude more code, and it will never stop growing, since
      each new assertion requires a new matcher or method that pairs the logic
      with a natural-language string describing the logic.
   1. Not everyone speaks the same natural language, but such features must
      either be monolingual, or grow even an order of magnitude _bigger_ to
      support translation.
   1. It's too easy to make mistakes or assumptions that lead to error messages
      that are actually _not_ helpful descriptions of what the underlying code
      is doing.
   1. A large percentage of the time, a developer must look at the test code
      anyway, so no real time is saved.