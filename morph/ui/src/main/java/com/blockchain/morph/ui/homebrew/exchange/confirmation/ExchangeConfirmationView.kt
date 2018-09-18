package com.blockchain.morph.ui.homebrew.exchange.confirmation

import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface ExchangeConfirmationView : View {

    val clickEvents: Observable<Unit>

    fun continueToExchangeLocked()
}