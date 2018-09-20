package com.blockchain.morph.ui.homebrew.exchange.confirmation

import info.blockchain.balance.CryptoValue
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface ExchangeConfirmationView : View {

    val clickEvents: Observable<Unit>

    fun continueToExchangeLocked(transactionId: String)

    fun showSecondPasswordDialog()

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun displayErrorDialog()

    fun updateFee(cryptoValue: CryptoValue)
}