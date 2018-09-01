package com.blockchain.kycui.extensions

import io.reactivex.Observable

fun <T> Observable<T>.skipFirstUnless(predicate: (T) -> Boolean): Observable<T> =
    this.publish { upstream ->
        Observable.merge(
            upstream.take(1).filter(predicate),
            upstream.skip(1)
        )
    }