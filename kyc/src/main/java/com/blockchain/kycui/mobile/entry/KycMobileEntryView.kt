package com.blockchain.kycui.mobile.entry

import android.support.annotation.StringRes
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import com.blockchain.kycui.mobile.entry.models.PhoneNumber
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycMobileEntryView : View {

    val phoneNumberObservable: Observable<Pair<PhoneNumber, Unit>>

    fun preFillPhoneNumber(phoneNumber: String)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp(displayModel: PhoneDisplayModel)

    fun displayErrorDialog(@StringRes errorMessage: Int)
}
