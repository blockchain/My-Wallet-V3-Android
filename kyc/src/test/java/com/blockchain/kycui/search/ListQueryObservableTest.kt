package com.blockchain.kycui.search

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class ListQueryObservableTest {

    @Test
    fun `query change should trigger onNext`() {
        val querySubject = PublishSubject.create<CharSequence>()
        val testObserver = ListQueryObservable(querySubject, Observable.just(emptyList<Any>()))
            .matchingItems { query, _ -> listOf(query) }
            .test()

        testObserver.assertNoValues()
        querySubject.onNext("")
        testObserver.assertValue(listOf(""))
    }

    @Test
    fun `list change should trigger onNext`() {
        val query = Observable.just<CharSequence>("")
        val listObservable = PublishSubject.create<List<Any>>()

        val testObserver = ListQueryObservable(query, listObservable)
            .matchingItems { _, list -> list }
            .test()

        val list = listOf(Any())
        testObserver.assertNoValues()
        listObservable.onNext(list)
        testObserver.assertValue(list)
    }
}