package com.blockchain.kycui.onfidosplash

import android.support.annotation.StringRes
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface OnfidoSplashView : View {

    val uiState: Observable<Unit>

    fun continueToOnfido(apiKey: String, applicantId: String)

    fun showProgressDialog(cancelable: Boolean)

    fun dismissProgressDialog()

    fun continueToCompletion()

    fun showErrorToast(@StringRes message: Int)
}