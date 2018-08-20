package com.blockchain.kycui.mobile.entry

import android.support.annotation.StringRes
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycMobileEntryView : View {

    val phoneNumber: Observable<String>

    fun preFillPhoneNumber(phoneNumber: String)

    fun setButtonEnabled(enabled: Boolean)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp()

    fun finishPage()
}
