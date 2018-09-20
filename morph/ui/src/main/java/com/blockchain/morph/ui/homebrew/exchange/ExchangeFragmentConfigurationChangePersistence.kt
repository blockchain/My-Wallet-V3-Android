package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.morph.exchange.mvi.FieldUpdateIntent
import info.blockchain.balance.CryptoCurrency
import java.math.BigDecimal

class ExchangeFragmentConfigurationChangePersistence : ViewModel() {

    var currentValue: BigDecimal = BigDecimal.ZERO

    var from = CryptoCurrency.BTC
        set(value) {
            if (field == value) return
            currentValue = BigDecimal.ZERO
            field = value
        }

    var to = CryptoCurrency.ETHER
        set(value) {
            if (field == value) return
            currentValue = BigDecimal.ZERO
            field = value
        }

    var fieldMode = FieldUpdateIntent.Field.FROM_FIAT
}
