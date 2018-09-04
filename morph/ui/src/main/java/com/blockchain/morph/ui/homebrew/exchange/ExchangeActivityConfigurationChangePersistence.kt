package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.morph.exchange.mvi.FieldUpdateIntent
import info.blockchain.balance.CryptoCurrency

class ExchangeActivityConfigurationChangePersistence : ViewModel() {

    var currentValue: Long = 0

    var from = CryptoCurrency.BTC
        set(value) {
            if (field == value) {
                return
            }
            val newTo = if (value == to) field else to
            currentValue = 0
            field = value
            to = newTo
        }
    var to = CryptoCurrency.ETHER
        set(value) {
            if (field == value) {
                return
            }
            val newFrom = if (value == from) field else from
            currentValue = 0
            field = value
            from = newFrom
        }

    var fieldMode = FieldUpdateIntent.Field.FROM_FIAT
}
