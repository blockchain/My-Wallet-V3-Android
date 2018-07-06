@file:Suppress("unused")

package piuk.blockchain.androidcore.utils

import io.reactivex.Observable

sealed class Optional<out T> {
    class Some<out T>(val element: T) : Optional<T>()
    object None : Optional<Nothing>()
}

/**
 * Left is generally used as an Error or Empty status, Right is generally containing the value
 * requested.
 */
sealed class Either<out A, out B> {
    data class Left<out A>(val value: A) : Either<A, Nothing>()
    data class Right<out B>(val value: B) : Either<Nothing, B>()

    inline fun <L, R, T> Either<L, R>.fold(left: (L) -> T, right: (R) -> T): T =
        when (this) {
            is Left -> left(value)
            is Right -> right(value)
        }

    inline fun <L, R, T> Either<L, R>.flatMap(f: (R) -> Either<L, T>): Either<L, T> =
        fold({ this as Left }, f)

    inline fun <L, R, T> Either<L, R>.map(f: (R) -> T): Either<L, T> =
        flatMap { Right(f(it)) }
}

fun <T> Observable<T>.either(): Observable<Either<Throwable, T>> =
    map { Either.Right(it) as Either<Throwable, T> }.onErrorReturn { Either.Left(it) }
