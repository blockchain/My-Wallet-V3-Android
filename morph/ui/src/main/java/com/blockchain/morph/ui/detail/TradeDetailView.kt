package com.blockchain.morph.ui.detail

import android.support.annotation.StringRes
import piuk.blockchain.androidcoreui.ui.base.View
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import java.util.Locale

interface TradeDetailView : View {

    val depositAddress: String

    val locale: Locale

    fun updateUi(uiState: TradeDetailUiState)

    fun updateDeposit(label: String, amount: String)

    fun updateReceive(label: String, amount: String)

    fun updateExchangeRate(exchangeRate: String)

    fun updateTransactionFee(displayString: String)

    fun updateOrderId(displayString: String)

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType type: String)

    fun finishPage()

    fun showProgressDialog(@StringRes message: Int)

    fun dismissProgressDialog()
}