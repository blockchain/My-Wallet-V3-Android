package com.blockchain.balance

import android.support.annotation.DrawableRes
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.R

@DrawableRes
fun CryptoCurrency.layerListDrawableRes() =
    when (this) {
        CryptoCurrency.BTC -> R.drawable.layer_list_bitcoin
        CryptoCurrency.ETHER -> R.drawable.layer_list_eth
        CryptoCurrency.BCH -> R.drawable.layer_list_bitcoin_cash
    }