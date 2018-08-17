package com.blockchain.morph.exchange.mvi

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`should equal`
import org.junit.Test

class LatestPairTest {

    @Test
    fun empty() {
        Observable.empty<Any>()
            .latestPair(Observable.empty<Any>())
            .test()
            .values() `should equal` emptyList()
    }

    @Test
    fun `one in source 1`() {
        Observable.just(1)
            .latestPair(Observable.empty<Any>())
            .test()
            .values() `should equal` emptyList()
    }

    @Test
    fun `one in source 2`() {
        Observable.empty<Any>()
            .latestPair(Observable.just(1))
            .test()
            .values() `should equal` emptyList()
    }

    @Test
    fun `one in each source`() {
        Observable.just(1)
            .latestPair(Observable.just(2))
            .test()
            .values() `should equal` listOf(1 to 2)
    }

    @Test
    fun `two in second source`() {
        Observable.just("A")
            .latestPair(Observable.just(1, 2))
            .test()
            .values() `should equal` listOf("A" to 1, "A" to 2)
    }

    @Test
    fun `two in first source`() {
        val subject = PublishSubject.create<String>()
        val test = subject
            .latestPair(Observable.just(1))
            .test()
        subject.onNext("A")
        subject.onNext("B")
        test.values() `should equal` listOf("A" to 1, "B" to 1)
    }
}
