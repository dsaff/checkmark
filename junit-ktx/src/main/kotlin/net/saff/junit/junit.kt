package net.saff.junit

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

fun <T : TestRule, U> T.wrap(fn: (T) -> U): U {
    return extract {
        apply(object : Statement() {
            override fun evaluate() {
                yield(fn(this@wrap))
            }
        }, Description.EMPTY).evaluate()
    }
}

interface ExtractScope<T> {
    fun yield(t: T)
}

fun <T> extract(fn: ExtractScope<T>.() -> Unit): T {
    var value: T? = null
    object: ExtractScope<T> {
        override fun yield(t: T) {
            value = t
        }
    }.fn()
    return value!!
}