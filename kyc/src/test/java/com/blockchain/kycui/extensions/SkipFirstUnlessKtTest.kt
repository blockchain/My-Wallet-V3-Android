package com.blockchain.kycui.extensions

import io.reactivex.rxkotlin.toObservable
import org.amshove.kluent.`should equal`
import org.junit.Test

class SkipFirstUnlessKtTest {

    @Test
    fun `should filter out initial empty string`() {
        val source = listOf("", "one", "two", "three", "four").toObservable()
        val testObserver = source.skipFirstUnless { !it.isEmpty() }.test()
        val list = testObserver.values()
        list `should equal` listOf("one", "two", "three", "four")
    }

    @Test
    fun `should not filter out second item matching condition`() {
        val source = listOf("one", "", "two", "three", "four").toObservable()
        val testObserver = source.skipFirstUnless { !it.isEmpty() }.test()
        val list = testObserver.values()
        list `should equal` listOf("one", "", "two", "three", "four")
    }

    @Test
    fun `should only filter initially matching item`() {
        val source = listOf("", "", "", "", "").toObservable()
        val testObserver = source.skipFirstUnless { !it.isEmpty() }.test()
        val list = testObserver.values()
        list `should equal` listOf("", "", "", "")
    }

    @Test
    fun `should only filter no items`() {
        val source = listOf(1, 2, 3, 4, 5).toObservable()
        val testObserver = source.skipFirstUnless { it == 1 }.test()
        val list = testObserver.values()
        list `should equal` listOf(1, 2, 3, 4, 5)
    }
}