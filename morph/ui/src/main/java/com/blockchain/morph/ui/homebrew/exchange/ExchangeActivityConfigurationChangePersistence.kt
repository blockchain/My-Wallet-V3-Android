package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.morph.exchange.mvi.FieldUpdateIntent
import info.blockchain.balance.CryptoCurrency
import java.math.BigDecimal

class ExchangeActivityConfigurationChangePersistence : ViewModel() {

    var currentValue: BigDecimal = BigDecimal.ZERO

    var from = CryptoCurrency.BTC
        set(value) {
            if (field == value) {
                return
            }
            val newTo = if (value == to) field else to
            currentValue = BigDecimal.ZERO
            field = value
            to = newTo
        }
    var to = CryptoCurrency.ETHER
        set(value) {
            if (field == value) {
                return
            }
            val newFrom = if (value == from) field else from
            currentValue = BigDecimal.ZERO
            field = value
            from = newFrom
        }

    var fieldMode = FieldUpdateIntent.Field.FROM_FIAT
}
