@file:Suppress("NOTHING_TO_INLINE")

package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

inline fun <reified T> printEvent(tag: String, success: T?, error: Throwable?) =
    when {
        success == null && error == null -> Timber.d("$tag Complete") /* Only with Maybe */
        success != null -> Timber.d("$tag Success $success")
        error != null -> Timber.d("$tag Error $error")
        else -> Unit
    }

inline fun printEvent(tag: String, error: Throwable?) =
    when {
        error != null -> Timber.d("$tag Error $error")
        else -> Timber.d("$tag Complete")
    }

inline fun tag() =
    Thread.currentThread().stackTrace
        .first { it.fileName.endsWith(".kt") }
        .let { stack -> "${stack.fileName.removeSuffix(".kt")}::${stack.methodName}:${stack.lineNumber}" }

inline fun <reified T> Single<T>.log(): Single<T> {
    val tag = tag()
    return doOnEvent { success, error -> printEvent(tag, success, error) }
        .doOnSubscribe { Timber.d("$tag Subscribe") }
        .doOnDispose { Timber.d("$tag Dispose") }
}

inline fun <reified T> Maybe<T>.log(): Maybe<T> {
    val tag = tag()
    return doOnEvent { success, error -> printEvent(tag, success, error) }
        .doOnSubscribe { Timber.d("$tag Subscribe") }
        .doOnDispose { Timber.d("$tag Dispose") }
}

inline fun Completable.log(): Completable {
    val tag = tag()
    return doOnEvent { printEvent(tag, it) }
        .doOnSubscribe { Timber.d("$tag Subscribe") }
        .doOnDispose { Timber.d("$tag Dispose") }
}

inline fun <reified T> Observable<T>.log(): Observable<T> {
    val line = tag()
    return doOnEach { Timber.d("$line Each $it") }
        .doOnSubscribe { Timber.d("$line Subscribe") }
        .doOnDispose { Timber.d("$line Dispose") }
}

inline fun <reified T> Flowable<T>.log(): Flowable<T> {
    val line = tag()
    return doOnEach { Timber.d("$line Each $it") }
        .doOnSubscribe { Timber.d("$line Subscribe") }
        .doOnCancel { Timber.d("$line Cancel") }
}