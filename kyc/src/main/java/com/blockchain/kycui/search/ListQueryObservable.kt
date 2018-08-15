package com.blockchain.kycui.search

import io.reactivex.Observable
import io.reactivex.functions.BiFunction

class ListQueryObservable<T>(
    private val queryObservable: Observable<CharSequence>,
    private val listObservable: Observable<List<T>>
) {

    fun matchingItems(
        filter: (CharSequence, List<T>) -> List<T>
    ): Observable<List<T>> =
        Observable.combineLatest(
            listObservable,
            queryObservable,
            BiFunction { list, input -> filter(input, list) }
        )
}
