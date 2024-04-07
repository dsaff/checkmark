package net.saff.junit

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

fun <T : TestRule, U> T.wrap(fn: (T) -> U): U {
    return extract {
        apply(object : Statement() {
            override fun evaluate() {
                yield(fn(this@wrap))
            }
        }, Description.EMPTY).evaluate()
    }
}

fun rule(fn: (Statement) -> Statement) = TestRule { base, _ -> fn(base) }

suspend inline fun <T> aura(crossinline block: Continuation<T>.() -> Unit): T {
    return suspendCoroutineUninterceptedOrReturn { cont ->
        cont.block()
        COROUTINE_SUSPENDED
    }
}

suspend fun <T : TestRule> T.scoped(): T {
    return aura { wrap { resume(this@scoped) } }
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