package com.blockchain.kycui.search

import io.reactivex.Observable
import io.reactivex.functions.BiFunction

class ListQueryObservable<T>(
    private val queryObservable: Observable<CharSequence>,
    private val listObservable: Observable<List<T>>
) {

    fun <T> matchingItems(
        filter: (CharSequence, List<T>) -> List<T>
    ): Observable<List<T>> =
        Observable.combineLatest(
            queryObservable,
            listObservable,
            BiFunction { input, list -> filter(input, list as List<T>) }
        )
}