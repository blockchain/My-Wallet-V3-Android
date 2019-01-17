package com.blockchain.koin

import com.blockchain.morph.trade.MergingMorphTradeDataManager
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import com.blockchain.morph.ui.homebrew.exchange.ExchangeModel
import com.blockchain.morph.ui.homebrew.exchange.confirmation.ExchangeConfirmationPresenter
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryPresenter
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.utils.PrefsUtil

val morphUiModule = applicationContext {

    bean { PrefsUtil(get()) }

    context("Payload") {

        factory { ExchangeConfirmationPresenter(get(), get(), get(), get(), get(), get()) }

        factory("merge") {
            MergingMorphTradeDataManager(
                get("nabu"),
                get("shapeshift")
            )
        }.bind(MorphTradeDataHistoryList::class)

        factory { TradeHistoryPresenter(get("merge"), get()) }

        context("Quotes") {

            factory { ExchangeModel(get(), get(), get(), get(), get(), get()) }
        }
    }

    apply { registerDebug() }
}
