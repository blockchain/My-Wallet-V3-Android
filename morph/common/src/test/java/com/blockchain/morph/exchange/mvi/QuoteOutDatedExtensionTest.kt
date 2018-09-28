package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.rxInit
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class QuoteOutDatedExtensionTest {

    private val states = PublishSubject.create<ExchangeViewState>()

    private val scheduler = TestScheduler()

    @get:Rule
    val rx = rxInit { computation(scheduler) }

    @Test
    fun `no quote update for 10 seconds results in clear quote`() {
        val test = states
            .quoteOutDated()
            .test()

        states.onNext(exchangeViewState(latestQuote = mock()))

        test.values() `should equal` emptyList<ClearQuoteIntent>()

        scheduler.advanceTimeBy(9, TimeUnit.SECONDS)
        scheduler.advanceTimeBy(999, TimeUnit.MILLISECONDS)

        test.values() `should equal` emptyList<ClearQuoteIntent>()

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)

        test.values() `should equal` listOf(ClearQuoteIntent)
    }

    @Test
    fun `an updated quote within 10 seconds resets the timer`() {
        val test = states
            .quoteOutDated()
            .test()

        states.onNext(exchangeViewState(latestQuote = mock()))

        test.values() `should equal` emptyList<ClearQuoteIntent>()

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        states.onNext(exchangeViewState(latestQuote = mock()))

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        test.values() `should equal` emptyList<ClearQuoteIntent>()

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        test.values() `should equal` listOf(ClearQuoteIntent)
    }

    @Test
    fun `the exact same quote within 10 seconds does not reset the timer`() {
        val test = states
            .quoteOutDated()
            .test()

        val latestQuote = mock<Quote>()
        states.onNext(exchangeViewState(latestQuote = latestQuote))

        test.values() `should equal` emptyList<ClearQuoteIntent>()

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        states.onNext(exchangeViewState(latestQuote = latestQuote))

        scheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        test.values() `should equal` listOf(ClearQuoteIntent)
    }
}

private fun exchangeViewState(latestQuote: Quote?): ExchangeViewState {
    return mock {
        on { this.latestQuote } `it returns` latestQuote
    }
}
