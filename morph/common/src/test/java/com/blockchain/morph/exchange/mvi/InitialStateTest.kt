package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`should equal`
import org.junit.Test

class InitialStateTest {

    @Test
    fun `initial state`() {
        val subject = PublishSubject.create<ExchangeIntent>()
        ExchangeDialog(subject, initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER))
            .viewModels
            .test()
            .assertValue {
                it.from `should equal` value(
                    userEntered(0.bitcoin()),
                    outOfDate(0.usd())
                )
                it.to `should equal` value(
                    outOfDate(0.ether()),
                    outOfDate(0.usd())
                )
                true
            }
    }
}
