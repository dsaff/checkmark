package net.saff.junit

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
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

object RunningInRings : CoroutineContext.Key<RunningInRings>, CoroutineContext.Element {
    override val key: CoroutineContext.Key<*>
        get() = this

    override fun toString(): String = "RunningInRings"
}

// SAFF: rename to rings??
fun rings(fn: suspend () -> Unit) {
    fn.startCoroutineUninterceptedOrReturn(object : Continuation<Any> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext + RunningInRings

        override fun resumeWith(result: Result<Any>) {
            result.getOrThrow()
        }
    })
}

suspend inline fun <T> ring(crossinline block: Continuation<T>.() -> Unit): T {
    return suspendCoroutineUninterceptedOrReturn { cont ->
        check(cont.context[RunningInRings] != null)
        cont.block()
        COROUTINE_SUSPENDED
    }
}

suspend fun <T : TestRule> T.ringed(): T {
    return ring { wrap { resume(this@ringed) } }
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