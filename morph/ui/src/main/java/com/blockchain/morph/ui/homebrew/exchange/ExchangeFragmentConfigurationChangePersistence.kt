package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import info.blockchain.balance.CryptoCurrency
import com.blockchain.accounts.AllAccountList
import com.blockchain.morph.exchange.mvi.Fix
import info.blockchain.balance.AccountReference
import java.math.BigDecimal

class ExchangeFragmentConfigurationChangePersistence(private val allAccountList: AllAccountList) : ViewModel() {

    var currentValue: BigDecimal = BigDecimal.ZERO

    var fromReference: AccountReference? = null
        set(value) {
            if (field == value) return
            currentValue = BigDecimal.ZERO
            field = value
        }

    var toReference: AccountReference? = null
        set(value) {
            if (field == value) return
            currentValue = BigDecimal.ZERO
            field = value
        }

    val from: AccountReference
        get() = fromReference ?: allAccountList[CryptoCurrency.BTC].defaultAccountReference()

    val to: AccountReference
        get() = toReference ?: allAccountList[CryptoCurrency.ETHER].defaultAccountReference()

    var fieldMode = Fix.BASE_FIAT
}
