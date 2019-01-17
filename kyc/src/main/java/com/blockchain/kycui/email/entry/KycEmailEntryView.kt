package com.blockchain.kycui.email.entry

import android.support.annotation.StringRes
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycEmailEntryView : View {

    val uiStateObservable: Observable<Pair<String, Unit>>

    fun preFillEmail(email: String)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp(email: String)
}
