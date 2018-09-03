package com.blockchain.koin

import com.blockchain.homebrew.FakeRatesStream
import com.blockchain.morph.ui.homebrew.exchange.RateStream
import org.koin.dsl.context.Context

fun Context.registerDebug() {

    factory { FakeRatesStream(get()) as RateStream }
}
