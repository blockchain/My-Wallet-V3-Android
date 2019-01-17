package com.blockchain.kycui.email.validation

import android.support.annotation.StringRes
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycEmailValidationView : View {

    val uiStateObservable: Observable<Pair<String, Unit>>

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun displayErrorDialog(@StringRes message: Int)

    fun theEmailWasResent()

    fun setVerified(verified: Boolean)
}
