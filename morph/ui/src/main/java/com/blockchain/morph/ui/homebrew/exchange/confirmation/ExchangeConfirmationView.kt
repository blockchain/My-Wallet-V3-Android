package com.blockchain.morph.ui.homebrew.exchange.confirmation

import android.support.annotation.StringRes
import com.blockchain.morph.exchange.mvi.ExchangeViewModel
import com.blockchain.morph.ui.homebrew.exchange.locked.ExchangeLockedModel
import info.blockchain.balance.CryptoValue
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface ExchangeConfirmationView : View {

    val locale: Locale

    val clickEvents: Observable<ExchangeViewModel>

    fun continueToExchangeLocked(lockedModel: ExchangeLockedModel)

    fun showSecondPasswordDialog()

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun displayErrorDialog()

    fun updateFee(cryptoValue: CryptoValue)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType type: String)
}