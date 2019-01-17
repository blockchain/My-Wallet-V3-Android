package com.blockchain.kycui.mobile.validation

import android.support.annotation.StringRes
import com.blockchain.kycui.mobile.entry.models.PhoneVerificationModel
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import piuk.blockchain.androidcoreui.ui.base.View

interface KycMobileValidationView : View {

    val uiStateObservable: Observable<Pair<PhoneVerificationModel, Unit>>

    val resendObservable: Observable<Pair<PhoneNumber, Unit>>

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun continueSignUp()

    fun displayErrorDialog(@StringRes message: Int)

    fun theCodeWasResent()
}
