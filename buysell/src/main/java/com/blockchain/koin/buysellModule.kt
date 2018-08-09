package com.blockchain.koin

import com.blockchain.network.modules.MoshiBuilderInterceptor
import com.squareup.moshi.Moshi
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidbuysell.models.coinify.BuyFrequencyAdapter
import piuk.blockchain.androidbuysell.models.coinify.CannotTradeReasonAdapter
import piuk.blockchain.androidbuysell.models.coinify.DetailsAdapter
import piuk.blockchain.androidbuysell.models.coinify.GrantTypeAdapter
import piuk.blockchain.androidbuysell.models.coinify.MediumAdapter
import piuk.blockchain.androidbuysell.models.coinify.ReviewStateAdapter
import piuk.blockchain.androidbuysell.models.coinify.TradeStateAdapter
import piuk.blockchain.androidbuysell.models.coinify.TransferStateAdapter

val buySellModule = applicationContext {

    bean("buySell") {
        object : MoshiBuilderInterceptor {
            override fun intercept(builder: Moshi.Builder) {
                builder
                    .add(CannotTradeReasonAdapter())
                    .add(ReviewStateAdapter())
                    .add(MediumAdapter())
                    .add(TradeStateAdapter())
                    .add(TransferStateAdapter())
                    .add(DetailsAdapter())
                    .add(GrantTypeAdapter())
                    .add(BuyFrequencyAdapter())
            }
        } as MoshiBuilderInterceptor
    }
}
