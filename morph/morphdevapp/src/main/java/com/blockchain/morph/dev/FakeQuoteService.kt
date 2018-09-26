package com.blockchain.morph.dev

import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class FakeQuoteService : QuoteService {
    override val rates: Observable<ExchangeRate>
        get() = TODO("not implemented")

    private val subject = PublishSubject.create<Quote>()

    override fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest) {
        subject.onNext(
            when (quoteRequest) {
                is ExchangeQuoteRequest.Selling ->
                    Quote(
                        fix = Fix.BASE_CRYPTO,
                        from = Quote.Value(
                            quoteRequest.offering,
                            FiatValue.fromMajor(
                                "USD",
                                quoteRequest.offering.toBigDecimal() * 12.34.toBigDecimal()
                            )
                        ),
                        to = Quote.Value(
                            quoteRequest.wanted.withMajorValue(quoteRequest.offering.toBigDecimal() * 2.3456.toBigDecimal()),
                            FiatValue.fromMajor(
                                "USD",
                                quoteRequest.offering.toBigDecimal() * 11.34.toBigDecimal()
                            )
                        )
                    )
                is ExchangeQuoteRequest.SellingFiatLinked ->
                    Quote(
                        fix = Fix.BASE_FIAT,
                        from = Quote.Value(
                            quoteRequest.offering.withMajorValue(
                                quoteRequest.offeringFiatValue.toBigDecimal() / 12.3456.toBigDecimal()
                            ),
                            quoteRequest.offeringFiatValue
                        ),
                        to = Quote.Value(
                            quoteRequest.wanted.withMajorValue(
                                quoteRequest.offeringFiatValue.toBigDecimal() / 2.3456.toBigDecimal()
                            ),
                            FiatValue.fromMajor(
                                "USD",
                                quoteRequest.offeringFiatValue.toBigDecimal() / 11.34.toBigDecimal()
                            )
                        )
                    )
                else -> TODO()
            }
        )
    }

    override fun openAsDisposable(): Disposable {
        return CompositeDisposable()
    }

    override val quotes: Observable<Quote>
        get() = subject.delay(1, TimeUnit.SECONDS)

    override val connectionStatus: Observable<QuoteService.Status>
        get() = Observable.just(QuoteService.Status.Open)
}
