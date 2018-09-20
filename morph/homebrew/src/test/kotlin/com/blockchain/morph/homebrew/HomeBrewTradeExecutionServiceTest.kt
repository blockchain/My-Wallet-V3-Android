package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.nabu.api.NabuTransaction
import com.blockchain.nabu.api.TradeRequest
import com.blockchain.nabu.service.NabuMarketsService
import com.blockchain.serialization.BigDecimalAdaptor
import com.blockchain.testutils.getStringFromResource
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.squareup.moshi.Moshi
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class HomeBrewTradeExecutionServiceTest {

    private val nabuTransaction = mock<NabuTransaction>()

    private val marketsService: NabuMarketsService = mock {
        on { executeTrade(any()) } `it returns` Single.just(nabuTransaction)
    }

    private val homeBrewTradeExecutionService = HomeBrewTradeExecutionService(marketsService)

    @Test
    fun `executing a trade with a null raw quote throws`() {
        {
            homeBrewTradeExecutionService
                .executeTrade(
                    Quote(
                        from = mock(),
                        to = mock(),
                        rawQuote = null
                    ),
                    "",
                    ""
                )
        } `should throw the Exception` IllegalArgumentException::class `with message` "No quote supplied"
        verifyZeroInteractions(marketsService)
    }

    @Test
    fun `executing a trade without a QuoteJson quote throws`() {
        {
            homeBrewTradeExecutionService
                .executeTrade(
                    Quote(
                        from = mock(),
                        to = mock(),
                        rawQuote = Any()
                    ),
                    "",
                    ""
                )
        } `should throw the Exception` IllegalArgumentException::class `with message` "Quote is not expected type"
        verifyZeroInteractions(marketsService)
    }

    @Test
    fun `executing a trade forwards all the expected data, including the full original quote`() {
        val quote = loadQuoteJsonFromResource("quotes/quote_receive.json")
        homeBrewTradeExecutionService
            .executeTrade(
                Quote(
                    from = mock(),
                    to = mock(),
                    rawQuote = quote
                ),
                destinationAddress = "1destAddress",
                refundAddress = "1refundAddress"
            )

        verify(marketsService)
            .executeTrade(
                TradeRequest(
                    destinationAddress = "1destAddress",
                    refundAddress = "1refundAddress",
                    quote = quote
                )
            )
    }

    @Test
    fun `the result of executing a trade is just direct from marketsService`() {
        val quote = loadQuoteJsonFromResource("quotes/quote_receive.json")
        homeBrewTradeExecutionService
            .executeTrade(
                Quote(
                    from = mock(),
                    to = mock(),
                    rawQuote = quote
                ),
                destinationAddress = "1destAddress",
                refundAddress = "1refundAddress"
            )
            .test()
            .assertValue(nabuTransaction)
    }

    private fun loadQuoteJsonFromResource(filePath: String): QuoteJson {
        val moshi = Moshi.Builder()
            .add(BigDecimalAdaptor())
            .build()
        val jsonAdapter = moshi.adapter(QuoteJson::class.java)
        return jsonAdapter.fromJson(getStringFromResource(filePath))
            ?: throw IllegalStateException("Error parsing JSON")
    }
}
