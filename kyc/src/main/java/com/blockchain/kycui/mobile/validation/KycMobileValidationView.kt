package com.blockchain.kycui.mobile.validation

import android.support.annotation.StringRes
import com.blockchain.kycui.mobile.entry.models.PhoneVerificationModel
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycMobileValidationView : View {

    val verificationObservable: Observable<Pair<PhoneVerificationModel, Unit>>

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun continueSignUp()

    fun showErrorToast(@StringRes message: Int)

    fun displayErrorDialog(@StringRes message: Int)
}
