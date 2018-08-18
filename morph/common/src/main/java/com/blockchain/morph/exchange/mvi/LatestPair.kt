package com.blockchain.morph.exchange.mvi

import io.reactivex.Observable
import io.reactivex.functions.BiFunction

fun <T1, T2> Observable<T1>.latestPair(other: Observable<T2>): Observable<Pair<T1, T2>> =
    Observable.combineLatest(this, other, BiFunction { a, b -> a to b })
