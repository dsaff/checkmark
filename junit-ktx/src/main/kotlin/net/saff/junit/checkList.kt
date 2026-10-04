package net.saff.junit

import net.saff.checkmark.Checkmark.Companion.check

fun <T> List<T>.checkList(vararg expected: T): List<T> {
    val expectList = listOf(*expected)
    return check {
        it == expectList
    }
}
