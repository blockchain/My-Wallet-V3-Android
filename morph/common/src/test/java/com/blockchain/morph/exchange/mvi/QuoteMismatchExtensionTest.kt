package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.rxInit
import com.blockchain.testutils.usd
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.Money
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class QuoteMismatchExtensionTest {

    private val states = PublishSubject.create<ExchangeViewState>()

    private val scheduler = TestScheduler()

    @get:Rule
    val rx = rxInit { computation(scheduler) }

    @Test
    fun `if the quote matches, it should never clear`() {
        val test = states
            .quoteMismatch()
            .test()

        givenMatchingQuote()

        scheduler.advanceTimeBy(1, TimeUnit.HOURS)

        test.values() `should equal` emptyList<ClearQuoteIntent>()
    }

    @Test
    fun `if the quote doesn't match, it should clear after 2 seconds`() {
        val test = states
            .quoteMismatch()
            .test()

        givenMismatchQuote()

        scheduler.advanceTimeBy(249, TimeUnit.MILLISECONDS)
        test.values() `should equal` emptyList<ClearQuoteIntent>()
        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS)
        test.values() `should equal` listOf(ClearQuoteIntent)
    }

    @Test
    fun `if the quote doesn't match, but a matching quote comes in after, it should not clear`() {
        val test = states
            .quoteMismatch()
            .test()

        givenMismatchQuote()

        scheduler.advanceTimeBy(249, TimeUnit.MILLISECONDS)
        givenMatchingQuote()
        scheduler.advanceTimeBy(1, TimeUnit.HOURS)
        test.values() `should equal` emptyList<ClearQuoteIntent>()
    }

    private fun givenMatchingQuote() {
        states.onNext(exchangeViewState(lastQuoteValue = 10.usd(), userValue = 10.usd()))
    }

    private fun givenMismatchQuote() {
        states.onNext(exchangeViewState(lastQuoteValue = 10.usd(), userValue = 11.usd()))
    }
}

private fun exchangeViewState(userValue: Money, lastQuoteValue: Money): ExchangeViewState {
    val mockQuote: Quote = mock {
        on { fixValue } `it returns` lastQuoteValue
    }
    return mock {
        on { latestQuote } `it returns` mockQuote
        on { lastUserValue } `it returns` userValue
    }
}
