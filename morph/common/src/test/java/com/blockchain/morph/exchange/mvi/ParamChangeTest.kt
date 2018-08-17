package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ParamChangeTest {

    @Test
    fun `initial state`() {
        val initial = Params(
            CryptoCurrency.BTC,
            CryptoCurrency.BCH,
            "USD"
        )
        Observable.empty<ParamChangeIntent>().paramsDialog(initial)
            .test()
            .values().single() `should be` initial
    }

    @Test
    fun `update from crypto`() {
        Observable.just<ParamChangeIntent>(
            ParamChangeIntent.ChangeFromCrypto(
                CryptoCurrency.ETHER
            )
        )
            .paramsDialog(
                Params(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    "USD"
                )
            )
            .test()
            .values().last().from `should equal` CryptoCurrency.ETHER
    }

    @Test
    fun `update to crypto`() {
        Observable.just<ParamChangeIntent>(
            ParamChangeIntent.ChangeToCrypto(
                CryptoCurrency.ETHER
            )
        )
            .paramsDialog(
                Params(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    "USD"
                )
            )
            .test()
            .values().last().to `should equal` CryptoCurrency.ETHER
    }

    @Test
    fun `update fiat`() {
        Observable.just<ParamChangeIntent>(
            ParamChangeIntent.ChangeFiat(
                "GBP"
            )
        )
            .paramsDialog(
                Params(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    "USD"
                )
            )
            .test()
            .values().last().fiat `should equal` "GBP"
    }

    @Test
    fun `update all`() {
        Observable.just<ParamChangeIntent>(
            ParamChangeIntent.ChangeFiat("GBP"),
            ParamChangeIntent.ChangeFromCrypto(CryptoCurrency.BCH),
            ParamChangeIntent.ChangeToCrypto(CryptoCurrency.ETHER)
        ).paramsDialog(
            Params(
                CryptoCurrency.BTC,
                CryptoCurrency.BCH,
                "USD"
            )
        )
            .test()
            .values() `should equal` listOf(
            Params(
                CryptoCurrency.BTC,
                CryptoCurrency.BCH,
                "USD"
            ),
            Params(
                CryptoCurrency.BTC,
                CryptoCurrency.BCH,
                "GBP"
            ),
            Params(
                CryptoCurrency.BCH,
                CryptoCurrency.BCH,
                "GBP"
            ),
            Params(
                CryptoCurrency.BCH,
                CryptoCurrency.ETHER,
                "GBP"
            )
        )
    }
}
